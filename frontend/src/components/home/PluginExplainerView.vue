<template>
    <div style="display: flex;">
      <cv-tile kind="standard" style="margin-right: 1.5%;">
        <div class="card-container">
          <div class="small-header">
            AVAILABLE TODAY
          </div>
          <div class="icon-and-title-container">
            <Script24/>
            <div class="title">
              Generate a CBOM with our Cryptography Plugin for Sonar™
            </div>
          </div>
          <p class="body-text">{{ textPlugin }}</p>
          <div class="button-link-trademark">
            <p style="padding-top: 6px; font-size: smaller; color: var(--cds-text-secondary);">
              Sonar™ and SonarQube™ are trademarks of SonarSource SA.
              <br/>
              The Cryptography Plugin is not a SonarSource SA product.
            </p>
            <cv-button kind="secondary" :icon="LogoGithub20" @click="openLink()" style="margin-left: auto;">
              See on GitHub
            </cv-button>
          </div>
        </div>
      </cv-tile>
  
      <cv-tile kind="standard" style="margin-left: 1.5%;">
        <div class="card-container">
          <div class="small-header">
            COMING SOON
          </div>
          <div class="icon-and-title-container">
            <SecurityServices24/>
            <div class="title">
              CBOM Generator Tool
            </div>
          </div>
          <p class="body-text">{{ textGenerator }}</p>
          <cv-button kind="primary" :icon="LogoGithub20" disabled style="margin-left: auto; margin-top: auto;">
            Coming soon on GitHub
          </cv-button>
        </div>
      </cv-tile>
    </div>
  </template>
  
  <script>
  import { model } from "@/model.js";
  import { Launch20, LogoGithub20, Script24, SecurityServices24 } from "@carbon/icons-vue";
  
  export default {
    name: "PluginExplainerView",
    data() {
      return {
        model,
        textPlugin: "Use the SonarQube™ software with our open-source plugin to scan your source code, automatically identify the use of cryptography and generate a CBOM. Currently available for Java and Python.",
        textGenerator: "You will soon be able to generate a CBOM for open source Git repositories by running our simple CBOM Generator Tool on your device.",
        url: "https://github.com/IBM/sonar-cryptography",
        Launch20,
        LogoGithub20
      };
    },
    components: {
      Script24,
      SecurityServices24
    },
    methods: {
      openLink: function () {
        window.open(this.url, "_blank", "noreferrer");
      },
    },
  };
  </script>
  
  <style scoped>
  .card-container {
    padding: 8px;
    height: 100%;
    display: flex;
    flex-direction: column;
  }
  .icon-and-title-container {
    display: flex;
    align-items: center;
    margin-bottom: 12px;
  }
  .title {
    font-size: large;
    font-weight: 400;
    padding-left: 8px;
  }
  .small-header {
    font-size: small;
    font-weight: 600;
    margin-bottom: 10px;
  }
  .body-text {
    padding-bottom: 16px;
  }
  .button-link-trademark {
    display: flex;
  }
  /* Responsive button-link-trademark and trademark text */
  @media (max-width: 1100px) {
    .button-link-trademark {
      flex-direction: column;
      gap: 14px;
    }
    .body-text {
      padding-bottom: 0px;
    }
  }
  </style>