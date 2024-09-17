<template>
    <div class="container">
      <div style="padding-right: 2px;">
        If you do not have a CBOM, visualize our
      </div>
      <cv-button :icon="ArrowRight24" kind="ghost" @click="showResultFromUpload(demoCbom, '')" style="padding: 3px;">
        sample CBOM file
      </cv-button>
    </div>
  </template>
  
  <script>
  import { model } from "@/model.js";
  import { ArrowRight24 } from "@carbon/icons-vue";
  import { showResultFromUpload } from "@/helpers";
  import demoCbom from '../../../resources/keycloak-cbom.json';
  
  export default {
    name: "TrySampleButton",
    data() {
      return {
        model,
        demoCbom,
        ArrowRight24,
      };
    },
    methods: {
      showResultFromUpload
    },
  };
  </script>
  
  <style>
  .container {
    display: flex;
    align-items: center;
  }
  </style>