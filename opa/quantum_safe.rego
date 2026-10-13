package policies

##################
# Helper functions
##################

is_algorithm(component) if {
	# component.type == "cryptographic-asset"
	component.cryptoProperties.assetType == "algorithm"
}

is_asymmetric(component) if {
	is_algorithm(component)
	asymmetric_primitives := ["signature", "keyagree", "kem", "pke", "unknown", "other"]
	component.cryptoProperties.algorithmProperties.primitive in asymmetric_primitives
}

in_whitelist(primitive, id, whitelist) := "quantum-safe" if {
	id in whitelist
} else := "unknown" if {
	primitive in ["unknown", "other"]
} else := "quantum-vulnerable"

at_least(value, ref) := "quantum-safe" if {
	value >= ref
} else := "quantum-vulnerable"

##################
# Rules
##################

# Mark async algorithms as "quantum-safe" or "quantum-vulnerable"
# if name is in whitelist or not
quantum_safe.findings contains finding if {
	some component in input.components
	is_asymmetric(component)
	not component.cryptoProperties.oid

	# whitelist
	qs_algorithms := [
		"ml-kem", "ml-dsa", "slh-dsa", "pqxdh",
		"bike", "mceliece", "frodokem", "hqc",
		"kyber", "ntru", "crystals", "falcon",
		"mayo", "sphincs", "xmss", "lms",
	]

	finding := {
		"rule": "asymmetric_quantum_safe",
		"result": in_whitelist(
			component.cryptoProperties.algorithmProperties.primitive,
			component.name,
			qs_algorithms,
		),
		"value": component.name,
		"referenceList": qs_algorithms,
		"bom-ref": component["bom-ref"],
		"property": "name",
	}
}

# Mark async algorithms as "quantum-safe" or "quantum-vulnerable"
# if oid is in whitelist or not
quantum_safe.findings contains finding if {
	some component in input.components
	is_asymmetric(component)

	# whitelist
	qs_oids := [
		"1.3.6.1.4.1.2.267.12.4.4", "1.3.6.1.4.1.2.267.12.6.5", "1.3.6.1.4.1.2.267.12.8.7",
		"1.3.9999.6.4.16", "1.3.9999.6.7.16", "1.3.9999.6.4.13", "1.3.9999.6.7.13",
		"1.3.9999.6.5.12", "1.3.9999.6.8.12", "1.3.9999.6.5.10", "1.3.9999.6.8.10",
		"1.3.9999.6.6.12", "1.3.9999.6.9.12", "1.3.9999.6.6.10", "1.3.9999.6.9.10",
		"1.3.6.1.4.1.22554.5.6.1", "1.3.6.1.4.1.22554.5.6.2", "1.3.6.1.4.1.22554.5.6.3",
	]

	finding := {
		"rule": "asymmetric_quantum_safe",
		"result": in_whitelist(
			component.cryptoProperties.algorithmProperties.primitive,
			component.cryptoProperties.oid,
			qs_oids,
		),
		"value": component.cryptoProperties.oid,
		"referenceList": qs_oids,
		"bom-ref": component["bom-ref"],
		"property": "cryptoProperties.oid",
	}
}

# Mark algorithms with nistQuantumSecurityLevel >= min
# "quantum-safe", otherwise "quantum-vulnerable"
quantum_safe.findings contains finding if {
	some component in input.components
	is_algorithm(component)

	# exists
	component.cryptoProperties.algorithmProperties.nistQuantumSecurityLevel

	# minimum nist qs level
	qs_min_nist_level := 1

	finding := {
		"rule": "nist_qs_level",
		"result": at_least(
			component.cryptoProperties.algorithmProperties.nistQuantumSecurityLevel,
			qs_min_nist_level,
		),
		"value": component.cryptoProperties.algorithmProperties.nistQuantumSecurityLevel,
		"referenceValue": qs_min_nist_level,
		"bom-ref": component["bom-ref"],
		"property": "nistQuantumSecurityLevel",
	}
}

# Mark symmetric algorithms as "na"
quantum_safe.findings contains finding if {
	some component in input.components
	is_algorithm(component)
	not is_asymmetric(component)

	finding := {
		"rule": "symmetric_na",
		"result": "NA",
		"value": component.cryptoProperties.algorithmProperties.primitive,
		"bom-ref": component["bom-ref"],
		"property": "algorithmProperties.primitive",
	}
}
