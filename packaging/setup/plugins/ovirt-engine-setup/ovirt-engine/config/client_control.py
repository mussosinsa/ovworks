#
# ovirt-engine-setup -- ovirt engine setup
#
# Copyright oVirt Authors
# SPDX-License-Identifier: Apache-2.0
#

"""Client serial number and source IP access-control setup plugin."""

import gettext
import ipaddress
import json
import os
import re

from otopi import constants as otopicons
from otopi import filetransaction
from otopi import plugin
from otopi import util

from ovirt_engine_setup import constants as osetupcons
from ovirt_engine_setup.engine import constants as oenginecons


def _(m):
    return gettext.dgettext(message=m, domain='ovirt-engine-setup')


_CLIENT_CONTROL_ENV = getattr(oenginecons, 'ClientControlEnv', None)
_ALLOWED_IPS_ENV = getattr(
    _CLIENT_CONTROL_ENV,
    'ALLOWED_IPS',
    'OVESETUP_CLIENT_CONTROL/allowedIps',
)
_SERIAL_NUMBER_ENV = getattr(
    _CLIENT_CONTROL_ENV,
    'SERIAL_NUMBER',
    'OVESETUP_CLIENT_CONTROL/serialNumber',
)
_ENCRYPTOR_CONFIG_PATH = getattr(
    oenginecons.FileLocations,
    'OVIRT_ENGINE_ENCRYPTOR_CONFIG',
    '/etc/ovirt-engine/encryptor/config.json',
)


@util.export
class Plugin(plugin.PluginBase):
    """Collect and persist the client-control settings."""

    _DEFAULT_SERIAL_NUMBER = 'saeoll20250322'
    _LOOPBACK_ADDRESS = '127.0.0.1'
    _SERIAL_PATTERN = re.compile(r'^[A-Za-z0-9._-]{1,128}$')
    _REQUIRE_IP_PATTERN = re.compile(
        r'^\s*Require\s+ip\s+(.+?)\s*$',
        re.IGNORECASE,
    )

    def __init__(self, context):
        super(Plugin, self).__init__(context=context)

    @plugin.event(
        stage=plugin.Stages.STAGE_INIT,
    )
    def _init(self):
        self.environment.setdefault(
            _ALLOWED_IPS_ENV,
            None,
        )
        self.environment.setdefault(
            _SERIAL_NUMBER_ENV,
            None,
        )

    def _read_encryptor_config(self):
        path = _ENCRYPTOR_CONFIG_PATH
        if not os.path.exists(path):
            return {}
        try:
            with open(path, encoding='utf-8') as config_file:
                value = json.load(config_file)
            return value if isinstance(value, dict) else {}
        except (OSError, ValueError) as exception:
            raise RuntimeError(
                _(
                    'Unable to read client-control configuration: %s'
                ) % exception
            )

    def _read_allowed_ips(self):
        path = self.environment[
            oenginecons.ApacheEnv.HTTPD_CONF_OVIRT_ENGINE
        ]
        addresses = []
        if os.path.exists(path):
            with open(path, encoding='utf-8') as proxy_file:
                for line in proxy_file:
                    match = self._REQUIRE_IP_PATTERN.match(line)
                    if match:
                        addresses.extend(match.group(1).split())
        return addresses or [self._LOOPBACK_ADDRESS]

    def _normalize_allowed_ips(self, value):
        addresses = []
        for candidate in re.split(r'[\s,]+', value.strip()):
            if not candidate:
                continue
            try:
                normalized = str(ipaddress.ip_network(candidate, strict=False))
                if '/' not in candidate:
                    normalized = str(ipaddress.ip_address(candidate))
            except ValueError:
                raise RuntimeError(
                    _('Invalid client IP address or network: %s') % candidate
                )
            if normalized not in addresses:
                addresses.append(normalized)

        if self._LOOPBACK_ADDRESS not in addresses:
            addresses.insert(0, self._LOOPBACK_ADDRESS)
        return addresses

    @plugin.event(
        stage=plugin.Stages.STAGE_CUSTOMIZATION,
        condition=lambda self: self.environment[oenginecons.CoreEnv.ENABLE],
    )
    def _customization(self):
        encryptor_config = self._read_encryptor_config()

        if self.environment[
            _SERIAL_NUMBER_ENV
        ] is None:
            self.environment[
                _SERIAL_NUMBER_ENV
            ] = self.dialog.queryString(
                name='OVESETUP_CLIENT_CONTROL_SERIAL_NUMBER',
                note=_(
                    'Client serial number used for authentication '
                    '[@DEFAULT@]: '
                ),
                prompt=True,
                default=encryptor_config.get(
                    'serialNum',
                    self._DEFAULT_SERIAL_NUMBER,
                ),
            )

        serial_number = self.environment[
            _SERIAL_NUMBER_ENV
        ]
        if not self._SERIAL_PATTERN.match(serial_number):
            raise RuntimeError(
                _(
                    'Client serial number must contain 1-128 letters, '
                    'digits, dots, underscores, or hyphens'
                )
            )

        if self.environment[_ALLOWED_IPS_ENV] is None:
            self.environment[
                _ALLOWED_IPS_ENV
            ] = self.dialog.queryString(
                name='OVESETUP_CLIENT_CONTROL_ALLOWED_IPS',
                note=_(
                    'Client IP addresses or networks allowed to access the '
                    'engine (comma separated; 127.0.0.1 is always retained) '
                    '[@DEFAULT@]: '
                ),
                prompt=True,
                default=', '.join(self._read_allowed_ips()),
            )

        allowed_ips = self.environment[
            _ALLOWED_IPS_ENV
        ]
        if isinstance(allowed_ips, str):
            allowed_ips = self._normalize_allowed_ips(allowed_ips)
        else:
            allowed_ips = self._normalize_allowed_ips(','.join(allowed_ips))
        self.environment[
            _ALLOWED_IPS_ENV
        ] = allowed_ips

    @plugin.event(
        stage=plugin.Stages.STAGE_MISC,
        condition=lambda self: (
            self.environment[oenginecons.CoreEnv.ENABLE] and
            not self.environment[osetupcons.CoreEnv.DEVELOPER_MODE]
        ),
    )
    def _misc(self):
        path = _ENCRYPTOR_CONFIG_PATH
        config = self._read_encryptor_config()
        config['serialNum'] = self.environment[
            _SERIAL_NUMBER_ENV
        ]
        config_dir = os.path.dirname(path)
        if not os.path.isdir(config_dir):
            os.makedirs(config_dir, mode=0o700)

        self.environment[otopicons.CoreEnv.MAIN_TRANSACTION].append(
            filetransaction.FileTransaction(
                name=path,
                mode=0o600,
                owner=self.environment[osetupcons.SystemEnv.USER_ENGINE],
                enforcePermissions=True,
                content=json.dumps(config, indent=4, sort_keys=True) + '\n',
                modifiedList=self.environment[
                    otopicons.CoreEnv.MODIFIED_FILES
                ],
            )
        )
