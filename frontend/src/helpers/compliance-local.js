export function getLocalComplianceServiceName() {
    return "Basic Local Compliance Service";
}

export function createLocalComplianceReport(cbom) {
    const COMPLIANCE_SERVICE_NAME = getLocalComplianceServiceName();
    const POLICY_NAME = "NIST Post-Quantum Cryptography";
    const ASYMMETRIC_PRIMITIVES = ["signature", "key-agree", "kem", "pke"];
    const UNKNOWN_PRIMITIVES = ["unknown", "other"];
    const WHITELIST_NAMES = ["ml-kem", "ml-dsa", "slh-dsa", "pqxdh", "bike", "mceliece", "frodokem","hqc", "kyber", "ntru", "crystals", "falcon", "mayo", "sphincs", "xmss", "lms"];
    const WHITELIST_OIDS = [
        "1.3.6.1.4.1.2.267.12.4.4", "1.3.6.1.4.1.2.267.12.6.5", "1.3.6.1.4.1.2.267.12.8.7", "1.3.9999.6.4.16",
        "1.3.9999.6.7.16", "1.3.9999.6.4.13", "1.3.9999.6.7.13", "1.3.9999.6.5.12", "1.3.9999.6.8.12",
        "1.3.9999.6.5.10", "1.3.9999.6.8.10", "1.3.9999.6.6.12", "1.3.9999.6.9.12", "1.3.9999.6.6.10",
        "1.3.9999.6.9.10", "1.3.6.1.4.1.22554.5.6.1", "1.3.6.1.4.1.22554.5.6.2", "1.3.6.1.4.1.22554.5.6.3"
    ];

    const complianceLevels = [
        { id: 1, label: "Not Quantum Safe", colorHex: "#fac532", icon: "WARNING" },
        { id: 2, label: "Unknown", description: "Unknown Compliance", colorHex: "#17a9d1", icon: "UNKNOWN" },
        { id: 3, label: "Quantum Safe", colorHex: "green", icon: "CHECKMARK_SECURE" },
        { id: 4, label: "Not Applicable", description: "Not Applicable: we only categorize asymmetric algorithms", colorHex: "gray", icon: "NOT_APPLICABLE" }
    ];

    try {
        const findings = [];

        const components = cbom.components || [];
        components.forEach(component => {
            const bomRef = component["bom-ref"];
            if (!bomRef) {
                throw new Error("Missing bomRef field");
            }

            const type = component.type;
            if (!type || type !== "cryptographic-asset") {
                return;
            }

            let unknownFindingMessage = null;

            const cryptoProperties = component.cryptoProperties;
            if (cryptoProperties) {
                const algorithmProperties = cryptoProperties.algorithmProperties;
                if (algorithmProperties) {
                    const nistQuantumSecurityLevel = algorithmProperties.nistQuantumSecurityLevel;
                    if (nistQuantumSecurityLevel && nistQuantumSecurityLevel > 0) {
                        findings.push({
                            bomRef: bomRef,
                            levelId: 3,
                            message: "The field 'nistQuantumSecurityLevel' was set with a strictly positive value in the CBOM"
                        });
                        return;
                    }

                    const primitive = algorithmProperties.primitive;
                    if (primitive) {
                        if (ASYMMETRIC_PRIMITIVES.includes(primitive) || UNKNOWN_PRIMITIVES.includes(primitive)) {
                            const name = component.name;
                            const oid = cryptoProperties.oid;
                            if (oid && WHITELIST_OIDS.includes(oid)) {
                                findings.push({
                                    bomRef: bomRef,
                                    levelId: 3,
                                    message: "The OID of the asset is part of the Quantum Safe OIDs whitelist"
                                });
                                return;
                            }
                            if (name) {
                                const lowerCaseName = name.toLowerCase();
                                const matchedWhitelistItem = WHITELIST_NAMES.find(whitelistItem => lowerCaseName.includes(whitelistItem));
                                if (matchedWhitelistItem) {
                                    findings.push({
                                        bomRef: bomRef,
                                        levelId: 3,
                                        message: `The name of the asset contains '${matchedWhitelistItem}', which is part of the Quantum Safe whitelist of component names`
                                    });
                                    return;
                                }
                            }
                            if (ASYMMETRIC_PRIMITIVES.includes(primitive)) {
                                findings.push({
                                    bomRef: bomRef,
                                    levelId: 1,
                                    message: "The asset has an asymmetric primitive and does not match with the Quantum Safe whitelists of OIDs and names"
                                });
                                return;
                            } else {
                                // Primitive is part of UNKNOWN_PRIMITIVES
                                unknownFindingMessage = "The asset primitive is unclear and does not allow further categorization";
                            } 
                        } else {
                            findings.push({
                                bomRef: bomRef,
                                levelId: 4,
                                message: "The asset has a symmetric primitive, so the Quantum Safe categorization is not applicable"
                            });
                            return;
                        }
                    } else {
                        unknownFindingMessage = "The asset primitive was not set, which does not allow further categorization";
                    }
                } else {
                    unknownFindingMessage = "The field 'algorithmProperties' was not set, which does not allow further categorization";
                }
            } else {
                unknownFindingMessage = "The field 'cryptoProperties' was not set, which does not allow further categorization";
            }

            if (unknownFindingMessage) {
                findings.push({
                    bomRef: bomRef,
                    levelId: 2,
                    message: unknownFindingMessage
                });
            }
        });

        const globalComplianceStatus = findings.every(finding => finding.levelId !== 1 && finding.levelId !== 2);

        return {
            complianceServiceName: COMPLIANCE_SERVICE_NAME,
            policyName: POLICY_NAME,
            findings: findings,
            complianceLevels: complianceLevels,
            defaultComplianceLevel: 2,
            globalComplianceStatus: globalComplianceStatus,
            error: false
        };
    } catch (e) {
        console.error(e);
        return { error: true };
    }
}
