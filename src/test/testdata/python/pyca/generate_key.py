from cryptography.fernet import Fernet

# Generate a key
key = Fernet.generate_key()

# Save the key to a file
with open("../../../../../../../Downloads/pyca-cryptography-explorer-main/secret.key", "wb") as key_file:
    key_file.write(key)

print("Key saved:", key.decode())
