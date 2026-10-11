package policies_test

# Tests for opa/quantum_safe.rego
#
# Run with: opa test opa/ -v
#
# test_ecdh_* and test_x25519_* fail if the asymmetric primitives list
# uses "keyagree" instead of the CycloneDX value "key-agree".
# test_aes_* and test_mlkem_* are regression checks for the symmetric
# and KEM paths.

import data.policies.quantum_safe

# ---------------------------------------------------------------------------
# Shared test components
# ---------------------------------------------------------------------------

# A classical key-agreement algorithm (ECDH, secp256r1).
# CycloneDX 1.6 schema enum value is "key-agree" (with hyphen).
# The bug used "keyagree" (no hyphen), so this component fell through to
# the symmetric_na rule and was silently marked NA instead of quantum-vulnerable.
ecdh_component := {
	"type": "cryptographic-asset",
	"bom-ref": "alg-ecdh",
	"name": "ECDH",
	"cryptoProperties": {
		"assetType": "algorithm",
		"algorithmProperties": {
			"primitive": "key-agree",
			"curve": "secp256r1",
			"cryptoFunctions": ["keygen", "keyderive"],
		},
	},
}

# X25519 — another key-agreement algorithm, different curve.
# Included to confirm the fix covers the whole primitive, not just one name.
x25519_component := {
	"type": "cryptographic-asset",
	"bom-ref": "alg-x25519",
	"name": "X25519",
	"cryptoProperties": {
		"assetType": "algorithm",
		"algorithmProperties": {
			"primitive": "key-agree",
			"curve": "x25519",
			"cryptoFunctions": ["keygen", "keyderive"],
		},
	},
}

# ---------------------------------------------------------------------------
# Test 1: ECDH must be classified quantum-vulnerable, not NA
#
# Before fix: is_asymmetric() returns false for primitive "key-agree"
#             → symmetric_na fires → result "NA"   ← WRONG
# After fix:  is_asymmetric() returns true
#             → asymmetric_quantum_safe fires → result "quantum-vulnerable" ← CORRECT
# ---------------------------------------------------------------------------
test_ecdh_is_quantum_vulnerable if {
	findings := quantum_safe.findings with input as {"components": [ecdh_component]}

	# There must be exactly one finding for this component.
	count([f | some f in findings; f["bom-ref"] == "alg-ecdh"]) == 1

	# That finding must use the asymmetric rule, not symmetric_na.
	some finding in findings
	finding["bom-ref"] == "alg-ecdh"
	finding.rule == "asymmetric_quantum_safe"
	finding.result == "quantum-vulnerable"
}

# ---------------------------------------------------------------------------
# Test 2: X25519 must also be classified quantum-vulnerable, not NA
# ---------------------------------------------------------------------------
test_x25519_is_quantum_vulnerable if {
	findings := quantum_safe.findings with input as {"components": [x25519_component]}

	count([f | some f in findings; f["bom-ref"] == "alg-x25519"]) == 1

	some finding in findings
	finding["bom-ref"] == "alg-x25519"
	finding.rule == "asymmetric_quantum_safe"
	finding.result == "quantum-vulnerable"
}

# ---------------------------------------------------------------------------
# Regression: AES-256 (symmetric) must still be NA — the fix must not
# accidentally promote symmetric algorithms into the asymmetric path.
# ---------------------------------------------------------------------------
test_aes_remains_na if {
	aes_component := {
		"type": "cryptographic-asset",
		"bom-ref": "alg-aes256",
		"name": "AES-256",
		"cryptoProperties": {
			"assetType": "algorithm",
			"algorithmProperties": {
				"primitive": "block-cipher",
				"cryptoFunctions": ["encrypt", "decrypt"],
			},
		},
	}

	findings := quantum_safe.findings with input as {"components": [aes_component]}

	some finding in findings
	finding["bom-ref"] == "alg-aes256"
	finding.rule == "symmetric_na"
	finding.result == "NA"
}

# ---------------------------------------------------------------------------
# Regression: ML-KEM-768 (post-quantum KEM) must be quantum-safe.
# The "kem" primitive was already spelled correctly; confirm it still works.
# ---------------------------------------------------------------------------
test_mlkem_is_quantum_safe if {
	mlkem_component := {
		"type": "cryptographic-asset",
		"bom-ref": "alg-mlkem768",
		"name": "ml-kem",
		"cryptoProperties": {
			"assetType": "algorithm",
			"algorithmProperties": {
				"primitive": "kem",
				"parameterSetIdentifier": "768",
				"cryptoFunctions": ["encapsulate", "decapsulate"],
			},
		},
	}

	findings := quantum_safe.findings with input as {"components": [mlkem_component]}

	some finding in findings
	finding["bom-ref"] == "alg-mlkem768"
	finding.rule == "asymmetric_quantum_safe"
	finding.result == "quantum-safe"
}
