<template>
  <div>
    <cv-tile kind="standard">
      <div style="padding: 8px">
        <div style="display: flex; align-items: center; margin-bottom: 12px;">
          <Encryption24/>
          <div style="font-size: large; font-weight: 500; padding-left: 8px;">
            The Cryptography Bill of Materials
          </div>
        </div>
        <p>{{ text }}</p>
        <div class="buttons-container">
          <cv-button kind="secondary" :icon="LogoGithub20" @click="openLink(urlSpecification)" style="margin: 0px 3px;">
            Specification
          </cv-button>
          <cv-button kind="secondary" :icon="Blog20" @click="openLink(urlBlogPost)" style="margin: 0px 3px;">
            Blog post
          </cv-button>
          <cv-button kind="secondary" :icon="Launch20" @click="openLink(urlLearnMore)" style="margin: 0px 3px;">
            Learn more
          </cv-button>
        </div>
      </div>
    </cv-tile>
  </div>
</template>

<script>
import { model } from "@/model.js";
import { Launch20, LogoGithub20, Blog20, Encryption24 } from "@carbon/icons-vue";

export default {
  name: "ReturnButton",
  data() {
    return {
      model,
      text: "The Cryptography Bill of Materials (CBOM) is an object model that describes cryptographic assets and their dependencies, aiming to simplify the management of cryptography inventory and accelerate the migration to quantum-safe solutions. CycloneDX version 1.6, which incorporates the Cryptographic Bill of Materials (CBOM) capability, has recently achieved international recognition by being adopted as an ECMA standard. This development underscores the growing importance and acceptance of CycloneDX in the software supply chain security landscape. Overall, CBOM significantly enhances visibility into the cryptographic aspects of the software supply chain, enabling organizations to better manage risks, ensure compliance, and prepare for future cryptographic challenges, including the transition to quantum-safe cryptography.",
      urlSpecification: "https://github.com/CycloneDX/specification",
      urlBlogPost: "https://ecma-international.org/news/ecma-new-standard-ecma-424-on-cyclonedx-bill-of-materials/",
      urlLearnMore: "https://cyclonedx.org/capabilities/cbom/",
      Launch20,
      LogoGithub20,
      Blog20
    };
  },
  components: {
    Encryption24
  },
  methods: {
    openLink: function (url) {
      window.open(url, "_blank", "noreferrer");
    },
  },
};
</script>

<style>
.buttons-container {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>