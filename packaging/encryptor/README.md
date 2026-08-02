# oVirt configuration encryptor

The encryptor writes only the authenticated `OVENC001` format. It uses:

* AES-256-GCM with a random 256-bit data-encryption key for file contents;
* AES-256-GCM to wrap the data key;
* PBKDF2-HMAC-SHA-256 with 600,000 iterations and a random 128-bit salt to
  derive the wrapping key; and
* independent random 96-bit nonces for key wrapping and content encryption.

Both GCM operations authenticate the versioned header. Decryption fails without
writing output when the ciphertext, metadata, key, or authentication tag is
wrong.

## Key sources

Key sources are checked in this order:

1. `${CREDENTIALS_DIRECTORY}/ovirt-encryptor-passphrase` (systemd credential);
2. `OVIRT_ENCRYPTOR_PASSPHRASE` (environment variable); and
3. a mode-0600 file selected by `--secret-file` or `secret_file` in config.

Hardware identifiers, including MAC addresses, are never used. Prefer a systemd
credential. Environment variables are supported for compatibility but can be
exposed to privileged process inspection.

Example service override:

```ini
[Service]
LoadCredentialEncrypted=ovirt-encryptor-passphrase:/etc/credstore.encrypted/ovirt-encryptor-passphrase
```

`LoadCredentialEncrypted=` is a systemd unit directive, not an option that
`encrypt_conf_files.py` parses. systemd decrypts the credential and exposes it
as `${CREDENTIALS_DIRECTORY}/ovirt-encryptor-passphrase`; the script reads that
location before checking any fallback. The assignment must be on one logical
line. Splitting after `=` without a systemd continuation is invalid.

Create and install an encrypted credential as root, then add the packaged
drop-in example to the unit that actually invokes the encryptor:

```console
systemd-ask-password --no-tty 'oVirt encryptor passphrase' \
  | systemd-creds encrypt --name=ovirt-encryptor-passphrase - \
      /etc/credstore.encrypted/ovirt-encryptor-passphrase
install -D -m 0644 ovirt-encryptor-credential.conf.example \
  /etc/systemd/system/NAME.service.d/20-ovirt-encryptor-credential.conf
systemctl daemon-reload
```

Replace `NAME.service` with the oneshot or maintenance unit that runs
`encrypt_conf_files.py`. Do not add this directive to an unrelated service.
Interactive `engine-setup` and shell-launched `engine-backup` do not receive a
systemd credential automatically; invoke them from a credential-enabled unit or
use a mode-0600 secret file for those workflows.

## Commands

```console
encryptor.py --encrypt /etc/ovirt-engine/engine.conf.d/10-setup-database.conf
encryptor.py --decrypt --deny-legacy-cbc /etc/ovirt-engine/engine.conf.d/10-setup-database.conf
decrypt_conf.py --deny-legacy-cbc SOURCE OUTPUT
encrypt_conf_files.py --acknowledge-runtime-decryption
```

`encrypt_conf_files.py` processes only `10-setup-database.conf`,
`10-setup-dwh-database.conf`, and `internal.properties` below approved oVirt
directories. It does not follow symbolic links. Already-versioned files are
skipped.

The acknowledgement flag remains mandatory because runtime readers require the
same credential as the bulk encryptor. Python `ConfigFile` and Java
`ShellLikeConfd` decrypt approved DB files in memory; `ExtensionsManager`
provides encrypted `internal.properties` to AAA through a mode-0600 runtime
file below systemd's mode-0700 `RUNTIME_DIRECTORY`. Do not use the flag until
the credential drop-in has been deployed and Engine/SSO/AAA restart and login
tests have passed.

Legacy AES-256-CBC is read-only and disabled unless an explicit `legacy_cbc`
migration configuration supplies the old key and IV representation. Raw and
Base64 ciphertext, with an IV prefix or configured IV, are supported. Use
`--deny-legacy-cbc` after migration to prohibit it completely. New and migrated
files must always be written as `OVENC001`.
