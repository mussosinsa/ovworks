#!/bin/bash
###############################################################################
# OVirt Engine Security Audit Script
# Purpose: Perform comprehensive security checks on OVirt Engine installation
###############################################################################

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Audit result counters
PASS_COUNT=0
FAIL_COUNT=0
WARN_COUNT=0

# Log file
AUDIT_LOG="/var/log/ovirt-engine/security-audit-$(date +%Y%m%d-%H%M%S).log"
AUDIT_RESULTS="/tmp/ovirt-security-audit-results.json"

###############################################################################
# Logging Functions
###############################################################################

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$AUDIT_LOG"
}

log_pass() {
    echo -e "${GREEN}[PASS]${NC} $1" | tee -a "$AUDIT_LOG"
    ((PASS_COUNT++))
}

log_fail() {
    echo -e "${RED}[FAIL]${NC} $1" | tee -a "$AUDIT_LOG"
    ((FAIL_COUNT++))
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$AUDIT_LOG"
    ((WARN_COUNT++))
}

###############################################################################
# Security Check Functions
###############################################################################

check_file_permissions() {
    log_info "Checking critical file permissions..."

    # Check engine configuration files
    if [ -f "/etc/ovirt-engine/engine.conf" ]; then
        PERMS=$(stat -c "%a" /etc/ovirt-engine/engine.conf)
        if [ "$PERMS" == "600" ] || [ "$PERMS" == "640" ]; then
            log_pass "engine.conf has secure permissions ($PERMS)"
        else
            log_fail "engine.conf has insecure permissions ($PERMS), should be 600 or 640"
        fi
    fi

    # Check database password file
    if [ -f "/etc/ovirt-engine/.pgpass" ]; then
        PERMS=$(stat -c "%a" /etc/ovirt-engine/.pgpass)
        if [ "$PERMS" == "600" ]; then
            log_pass ".pgpass has secure permissions (600)"
        else
            log_fail ".pgpass has insecure permissions ($PERMS), must be 600"
        fi
    fi
}

check_ssl_certificates() {
    log_info "Checking SSL/TLS certificates..."

    CERT_PATH="/etc/pki/ovirt-engine/certs"
    if [ -d "$CERT_PATH" ]; then
        # Check certificate expiration
        for cert in "$CERT_PATH"/*.cer "$CERT_PATH"/*.pem; do
            if [ -f "$cert" ]; then
                EXPIRY=$(openssl x509 -enddate -noout -in "$cert" 2>/dev/null | cut -d= -f2)
                EXPIRY_EPOCH=$(date -d "$EXPIRY" +%s 2>/dev/null || echo 0)
                CURRENT_EPOCH=$(date +%s)
                DAYS_LEFT=$(( ($EXPIRY_EPOCH - $CURRENT_EPOCH) / 86400 ))

                if [ $DAYS_LEFT -gt 30 ]; then
                    log_pass "Certificate $(basename $cert) valid for $DAYS_LEFT days"
                elif [ $DAYS_LEFT -gt 0 ]; then
                    log_warn "Certificate $(basename $cert) expires in $DAYS_LEFT days"
                else
                    log_fail "Certificate $(basename $cert) has expired"
                fi
            fi
        done
    else
        log_warn "Certificate directory not found: $CERT_PATH"
    fi
}

check_database_security() {
    log_info "Checking database security settings..."

    # Check PostgreSQL connection encryption
    if command -v psql &> /dev/null; then
        DB_SSL=$(su - postgres -c "psql -d engine -c 'SHOW ssl;'" 2>/dev/null | grep -c "on" || echo 0)
        if [ "$DB_SSL" -gt 0 ]; then
            log_pass "Database SSL is enabled"
        else
            log_warn "Database SSL is not enabled"
        fi

        # Check password encryption
        DB_ENCRYPT=$(su - postgres -c "psql -d engine -c 'SHOW password_encryption;'" 2>/dev/null | grep -c "scram-sha-256" || echo 0)
        if [ "$DB_ENCRYPT" -gt 0 ]; then
            log_pass "Database password encryption is scram-sha-256"
        else
            log_warn "Database password encryption is not using scram-sha-256"
        fi
    else
        log_warn "psql command not available, skipping database checks"
    fi
}

check_network_security() {
    log_info "Checking network security settings..."

    # Check firewall status
    if command -v firewall-cmd &> /dev/null; then
        if firewall-cmd --state &> /dev/null; then
            log_pass "Firewall is active"

            # Check required ports
            HTTPS_OPEN=$(firewall-cmd --list-ports 2>/dev/null | grep -c "443/tcp" || echo 0)
            if [ "$HTTPS_OPEN" -gt 0 ]; then
                log_pass "HTTPS port (443) is open in firewall"
            else
                log_warn "HTTPS port (443) may not be open in firewall"
            fi
        else
            log_warn "Firewall is not active"
        fi
    fi

    # Check SELinux status
    if command -v getenforce &> /dev/null; then
        SELINUX_STATUS=$(getenforce)
        if [ "$SELINUX_STATUS" == "Enforcing" ]; then
            log_pass "SELinux is enforcing"
        elif [ "$SELINUX_STATUS" == "Permissive" ]; then
            log_warn "SELinux is in permissive mode"
        else
            log_fail "SELinux is disabled"
        fi
    fi
}

check_authentication_settings() {
    log_info "Checking authentication settings..."

    # Check AAA configuration
    AAA_CONFIG="/etc/ovirt-engine/aaa"
    if [ -d "$AAA_CONFIG" ]; then
        log_pass "AAA configuration directory exists"

        # Check for LDAP configuration
        if ls "$AAA_CONFIG"/*.properties &> /dev/null; then
            log_pass "Authentication providers configured"
        else
            log_warn "No authentication providers found"
        fi
    else
        log_warn "AAA configuration directory not found"
    fi
}

check_audit_logging() {
    log_info "Checking audit logging configuration..."

    # Check if audit log is enabled
    AUDIT_LOG_DIR="/var/log/ovirt-engine"
    if [ -d "$AUDIT_LOG_DIR" ]; then
        log_pass "Audit log directory exists"

        # Check recent audit activity
        if [ -f "$AUDIT_LOG_DIR/engine.log" ]; then
            RECENT_LOGS=$(find "$AUDIT_LOG_DIR/engine.log" -mtime -1 2>/dev/null | wc -l)
            if [ "$RECENT_LOGS" -gt 0 ]; then
                log_pass "Audit logging is active (logs from last 24 hours)"
            else
                log_warn "No recent audit logs found"
            fi
        fi
    else
        log_fail "Audit log directory not found"
    fi
}

check_service_security() {
    log_info "Checking service security..."

    # Check if engine is running as non-root
    if systemctl is-active ovirt-engine &> /dev/null; then
        ENGINE_USER=$(ps aux | grep ovirt-engine | grep -v grep | awk '{print $1}' | head -1)
        if [ "$ENGINE_USER" != "root" ]; then
            log_pass "Engine is running as non-root user ($ENGINE_USER)"
        else
            log_fail "Engine is running as root (security risk)"
        fi
    else
        log_warn "Engine service is not running"
    fi
}

check_integrity_checksums() {
    log_info "Checking file integrity checksums..."

    # Check critical JAR files
    ENGINE_LIB="/usr/share/ovirt-engine/modules"
    if [ -d "$ENGINE_LIB" ]; then
        JAR_COUNT=$(find "$ENGINE_LIB" -name "*.jar" 2>/dev/null | wc -l)
        if [ "$JAR_COUNT" -gt 0 ]; then
            log_pass "Found $JAR_COUNT engine JAR files"

            # Generate checksums for verification
            find "$ENGINE_LIB" -name "*.jar" -exec sha256sum {} \; > /tmp/ovirt-jar-checksums.txt 2>/dev/null
            log_info "Generated checksums for $JAR_COUNT JAR files"
        else
            log_warn "No JAR files found in engine library"
        fi
    else
        log_warn "Engine library directory not found"
    fi
}

check_backup_configuration() {
    log_info "Checking backup configuration..."

    # Check for backup configuration
    BACKUP_DIR="/var/lib/ovirt-engine-backup"
    if [ -d "$BACKUP_DIR" ]; then
        BACKUP_COUNT=$(find "$BACKUP_DIR" -name "*.tar.gz" -mtime -7 2>/dev/null | wc -l)
        if [ "$BACKUP_COUNT" -gt 0 ]; then
            log_pass "Found $BACKUP_COUNT recent backups (last 7 days)"
        else
            log_warn "No recent backups found (last 7 days)"
        fi
    else
        log_warn "Backup directory not found"
    fi
}

###############################################################################
# Main Execution
###############################################################################

main() {
    echo "========================================================================="
    echo "OVirt Engine Security Audit"
    echo "Date: $(date)"
    echo "========================================================================="
    echo ""

    # Create log directory if it doesn't exist
    mkdir -p "$(dirname $AUDIT_LOG)"

    # Run all security checks
    check_file_permissions
    echo ""
    check_ssl_certificates
    echo ""
    check_database_security
    echo ""
    check_network_security
    echo ""
    check_authentication_settings
    echo ""
    check_audit_logging
    echo ""
    check_service_security
    echo ""
    check_integrity_checksums
    echo ""
    check_backup_configuration
    echo ""

    # Generate summary
    echo "========================================================================="
    echo "Security Audit Summary"
    echo "========================================================================="
    echo -e "${GREEN}Passed: $PASS_COUNT${NC}"
    echo -e "${YELLOW}Warnings: $WARN_COUNT${NC}"
    echo -e "${RED}Failed: $FAIL_COUNT${NC}"
    echo ""
    echo "Detailed log: $AUDIT_LOG"
    echo ""

    # Generate JSON results
    cat > "$AUDIT_RESULTS" << EOF
{
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "summary": {
    "passed": $PASS_COUNT,
    "warnings": $WARN_COUNT,
    "failed": $FAIL_COUNT,
    "total": $((PASS_COUNT + WARN_COUNT + FAIL_COUNT))
  },
  "status": "$([ $FAIL_COUNT -eq 0 ] && echo "PASS" || echo "FAIL")",
  "log_file": "$AUDIT_LOG"
}
EOF

    echo "Results saved to: $AUDIT_RESULTS"

    # Return appropriate exit code
    if [ $FAIL_COUNT -gt 0 ]; then
        exit 1
    else
        exit 0
    fi
}

# Run main function
main "$@"
