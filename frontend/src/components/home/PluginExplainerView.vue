<template>
    <div style="display: flex;">
      <cv-tile kind="standard">
        <div class="card-container">
          <div class="icon-and-title-container">
            <Script24/>
            <div class="title">
              CBOMkit
            </div>
          </div>
          <p class="body-text">{{ textPlugin }}</p>
          <div class="button-link">
            <cv-button kind="secondary" :icon="LogoGithub20" @click="openLink()" style="margin-left: auto;">
              See on GitHub
            </cv-button>
          </div>
        </div>
      </cv-tile>
    </div> 
  </template>
  
  <script>
  import { model } from "@/model.js";
  import { Launch20, LogoGithub20, Script24 } from "@carbon/icons-vue";
  
  export default {
    name: "PluginExplainerView",
    data() {
      return {
        model,
        textPlugin: "CBOMkit is an open source toolset for working with Cryptography Bill of Materials. It can be used to generate CBOMs, visualise them, perform compliance checks and create a CBOM database. In particular, you can use it to scan source code by providing a link to a Git repository, then automatically detect the use of cryptography and generate a CBOM.",
        url: "https://github.com/IBM/cbomkit",
        Launch20,
        LogoGithub20
      };
    },
    components: {
      Script24,
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
    font-weight: 500;
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
  .button-link {
    display: flex;
  }
  </style>