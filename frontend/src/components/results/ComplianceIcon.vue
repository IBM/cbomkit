<template>
    <component
        :is="getComplianceIcon(asset)"
        :style="{fill: getComplianceColor(asset)}"
    />
  </template>
  
<script>
import { getComplianceIcon, getComplianceColor} from "@/helpers.js";
import {
    WatsonHealthImageAvailabilityUnavailable24,
    Security24,
    Checkmark24,
    WarningAlt24,
    NotAvailable24,
    MisuseOutline24
} from "@carbon/icons-vue";

export default {
    name: "ComplianceIcon",
    props: {
        asset: null,
    },
    components: {
        // Components which can be used for the compliance icon
        // All icons of `complianceIconMap` in `compliance.js` should be declared here
        WatsonHealthImageAvailabilityUnavailable24,
        Security24,
        Checkmark24,
        WarningAlt24,
        NotAvailable24,
        MisuseOutline24
    },
    methods: {
        getComplianceIcon,
        getComplianceColor,
    },
};
</script>
  