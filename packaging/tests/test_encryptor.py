import base64
import importlib.util
import stat
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest import mock


MODULE_PATH = Path(__file__).parents[1] / "encryptor" / "encryptor.py"
SPEC = importlib.util.spec_from_file_location("encryptor", MODULE_PATH)
encryptor = importlib.util.module_from_spec(SPEC)
try:
    SPEC.loader.exec_module(encryptor)
    CRYPTOGRAPHY_AVAILABLE = True
except ModuleNotFoundError as error:
    if error.name != "cryptography":
        raise
    CRYPTOGRAPHY_AVAILABLE = False


@unittest.skipUnless(CRYPTOGRAPHY_AVAILABLE, "python3-cryptography is not installed")
class EncryptorTest(unittest.TestCase):
    def setUp(self):
        self.passphrase = b"unit-test-passphrase"

    def test_round_trip_uses_versioned_gcm_format(self):
        plaintext = b'ENGINE_DB_PASSWORD="secret"\n'
        encrypted = encryptor.encrypt_bytes(plaintext, self.passphrase)
        self.assertTrue(encrypted.startswith(encryptor.MAGIC))
        self.assertEqual(encrypted[len(encryptor.MAGIC)], encryptor.VERSION)
        self.assertEqual(
            plaintext,
            encryptor.decrypt_bytes(encrypted, self.passphrase),
        )

    def test_decrypts_shared_java_interop_vector(self):
        vector = base64.b64decode(
            "T1ZFTkMwMDEBAAknwAABAgMEBQYHCAkKCwwNDg8QERITFBUWFxgZGhscHR4fICEi"
            "IyQlJicAMKCHF6zUoLJtPx0cDtBOcYes6QpztiMZSMLZBcqYBF0iu7+9G6dT6TOP"
            "eBPA17dzYgPsIZAoebwlz6I7YOg4Sazh8c+FE7nua6YPBTrKs159Z+gzrffyniMJ"
            "Yw=="
        )
        self.assertEqual(
            b'ENGINE_DB_PASSWORD="test"\n',
            encryptor.decrypt_bytes(
                vector,
                b"test-passphrase",
                deny_legacy_cbc=True,
            ),
        )

    def test_random_nonces_produce_different_ciphertexts(self):
        first = encryptor.encrypt_bytes(b"same", self.passphrase)
        second = encryptor.encrypt_bytes(b"same", self.passphrase)
        self.assertNotEqual(first, second)

    def test_tampering_and_wrong_key_are_rejected(self):
        encrypted = bytearray(encryptor.encrypt_bytes(b"secret", self.passphrase))
        encrypted[-1] ^= 1
        with self.assertRaisesRegex(encryptor.EncryptorError, "Authentication failed"):
            encryptor.decrypt_bytes(bytes(encrypted), self.passphrase)
        valid = encryptor.encrypt_bytes(b"secret", self.passphrase)
        with self.assertRaisesRegex(encryptor.EncryptorError, "Authentication failed"):
            encryptor.decrypt_bytes(valid, b"wrong")

    def test_truncated_and_legacy_data_are_rejected(self):
        with self.assertRaisesRegex(encryptor.EncryptorError, "truncated"):
            encryptor.decrypt_bytes(encryptor.MAGIC, self.passphrase)
        with self.assertRaisesRegex(encryptor.EncryptorError, "denied"):
            encryptor.decrypt_bytes(
                b"legacy",
                self.passphrase,
                deny_legacy_cbc=True,
            )

    def test_legacy_cbc_is_available_only_for_migration(self):
        key = bytes(range(32))
        iv = bytes(range(16))
        padder = encryptor.padding.PKCS7(128).padder()
        padded = padder.update(b"legacy secret") + padder.finalize()
        worker = encryptor.Cipher(
            encryptor.algorithms.AES(key),
            encryptor.modes.CBC(iv),
        ).encryptor()
        ciphertext = iv + worker.update(padded) + worker.finalize()
        config = {
            "legacy_cbc": {
                "enabled": True,
                "key": "hex:" + key.hex(),
                "iv_prefix": True,
            }
        }
        self.assertEqual(
            b"legacy secret",
            encryptor.decrypt_bytes(ciphertext, self.passphrase, config),
        )
        with self.assertRaisesRegex(encryptor.EncryptorError, "denied"):
            encryptor.decrypt_bytes(
                ciphertext,
                self.passphrase,
                config,
                deny_legacy_cbc=True,
            )

    def test_encrypt_migrates_legacy_instead_of_double_encrypting(self):
        key = bytes(range(32))
        iv = bytes(range(16))
        plaintext = b'ENGINE_DB_PASSWORD="legacy"\n'
        padder = encryptor.padding.PKCS7(128).padder()
        padded = padder.update(plaintext) + padder.finalize()
        worker = encryptor.Cipher(
            encryptor.algorithms.AES(key),
            encryptor.modes.CBC(iv),
        ).encryptor()
        legacy_ciphertext = iv + worker.update(padded) + worker.finalize()
        config = {
            "legacy_cbc": {
                "enabled": True,
                "key": "hex:" + key.hex(),
                "iv_prefix": True,
            }
        }
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "10-setup-database.conf"
            source.write_bytes(legacy_ciphertext)
            encryptor.transform_file(
                source,
                source,
                self.passphrase,
                config=config,
                allowed_roots=(root,),
            )
            self.assertEqual(
                plaintext,
                encryptor.decrypt_bytes(
                    source.read_bytes(),
                    self.passphrase,
                    deny_legacy_cbc=True,
                ),
            )

    def test_file_transform_is_atomic_and_preserves_metadata(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "config.conf"
            source.write_bytes(b"secret")
            source.chmod(0o640)
            roots = (root,)
            encryptor.transform_file(
                source,
                source,
                self.passphrase,
                allowed_roots=roots,
            )
            self.assertEqual(0o640, stat.S_IMODE(source.stat().st_mode))
            with self.assertRaisesRegex(encryptor.EncryptorError, "already"):
                encryptor.transform_file(
                    source,
                    source,
                    self.passphrase,
                    allowed_roots=roots,
                )
            encryptor.transform_file(
                source,
                source,
                self.passphrase,
                decrypt=True,
                deny_legacy_cbc=True,
                allowed_roots=roots,
            )
            self.assertEqual(b"secret", source.read_bytes())
            self.assertEqual(0o600, stat.S_IMODE(source.stat().st_mode))

    def test_existing_distinct_output_requires_overwrite(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "source.conf"
            output = root / "output.conf"
            source.write_bytes(b"secret")
            output.write_bytes(b"do not replace")
            with self.assertRaisesRegex(encryptor.EncryptorError, "Output exists"):
                encryptor.transform_file(
                    source,
                    output,
                    self.passphrase,
                    allowed_roots=(root,),
                )

    def test_atomic_config_creation_uses_mode_0600(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            config_path = root / "config.json"
            encryptor.atomic_update_config(
                config_path,
                {"active_format": "OVENC001"},
                allowed_roots=(root,),
            )
            self.assertEqual(
                0o600,
                stat.S_IMODE(config_path.stat().st_mode),
            )
            self.assertEqual(
                {"active_format": "OVENC001"},
                encryptor.json.loads(config_path.read_text(encoding="utf-8")),
            )

    def test_symlink_and_writable_input_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            target = root / "target"
            target.write_bytes(b"secret")
            link = root / "link"
            link.symlink_to(target)
            with self.assertRaisesRegex(encryptor.EncryptorError, "Symbolic|symbolic"):
                encryptor.validate_ovirt_path(link, allowed_roots=(root,))
            target.chmod(0o662)
            with self.assertRaisesRegex(encryptor.EncryptorError, "writable"):
                encryptor.validate_ovirt_path(target, allowed_roots=(root,))

    def test_secret_file_requires_mode_0600(self):
        with tempfile.TemporaryDirectory() as directory:
            secret = Path(directory) / "secret"
            secret.write_text("passphrase", encoding="utf-8")
            secret.chmod(0o644)
            with self.assertRaisesRegex(encryptor.EncryptorError, "0600"):
                encryptor._read_secret_file(secret)
            secret.chmod(0o600)
            self.assertEqual(b"passphrase", encryptor._read_secret_file(secret))

    def test_systemd_credential_is_the_primary_key_source(self):
        with tempfile.TemporaryDirectory() as directory:
            credential = Path(directory) / encryptor.DEFAULT_CREDENTIAL
            credential.write_bytes(b"systemd-passphrase\n")
            credential.chmod(0o600)
            environment = {
                "CREDENTIALS_DIRECTORY": directory,
                encryptor.PASSPHRASE_ENV: "environment-passphrase",
            }
            with mock.patch.dict(encryptor.os.environ, environment, clear=True):
                self.assertEqual(
                    b"systemd-passphrase",
                    encryptor.obtain_passphrase({}),
                )

    def test_bulk_cli_requires_runtime_decryption_acknowledgement(self):
        script = MODULE_PATH.with_name("encrypt_conf_files.py")
        result = subprocess.run(
            ["/usr/bin/python3", str(script)],
            check=False,
            capture_output=True,
            text=True,
        )
        self.assertEqual(2, result.returncode)
        self.assertIn("--acknowledge-runtime-decryption", result.stderr)

    def test_integration_review_covers_all_proposed_readers(self):
        review = (
            MODULE_PATH.parents[2]
            / "docs"
            / "config-file-decryption-integration-review.md"
        ).read_text(encoding="utf-8")
        for reader in (
            "configfile.py",
            "ShellLikeConfd.java",
            "ExtensionsManager.java",
            "Configuration.java",
        ):
            self.assertIn(reader, review)


if __name__ == "__main__":
    unittest.main()
