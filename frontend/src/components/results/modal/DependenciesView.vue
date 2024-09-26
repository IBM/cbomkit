<template>
  <div>
    <div>
      <h4 class="title">
        Depends on
      </h4>
      <div v-for="(asset, index) in dependsOn" :key="index">
        <p v-if="getAssetType(asset)">
          {{ getAssetType(asset) }}
        </p>
        <p v-if="getName(asset)">
          {{ getName(asset) }}
        </p>
        <p v-if="getBomRef(asset)">
          {{ getBomRef(asset) }}
        </p> 
        <cv-button
          kind="ghost"
          v-on:click="$emit('open-asset', asset)"
          style="margin-left: auto"
        >Open</cv-button>
      </div>
    </div>

    <h4 class="title">
      Provides to
    </h4>
    <!-- TODO -->
  </div>
</template>

<script>
import { getDependencies, getTermFullName } from "@/helpers.js";
// import { ArrowRight24 } from "@carbon/icons-vue";

export default {
  name: "DependenciesView",
  props: {
    bomRef: null,
  },
  data() {
    return {
    };
  },
  methods: {
    getDependencies,
    getTermFullName,
    getName: function(cryptoAsset) {
      if (cryptoAsset === undefined || cryptoAsset === null) {
        return "";
      }
      if (!Object.hasOwn(cryptoAsset, "name")) {
        return "";
      }
      return cryptoAsset.name;
    },
    getAssetType: function(cryptoAsset) {
      if (cryptoAsset === undefined || cryptoAsset === null) {
        return "";
      }
      if (!Object.hasOwn(cryptoAsset, "cryptoProperties")) {
        return "";
      }
      if (!Object.hasOwn(cryptoAsset.cryptoProperties, "assetType")) {
        return "";
      }
      return getTermFullName(cryptoAsset.cryptoProperties.assetType);
    },
    getBomRef: function(cryptoAsset) {
      if (cryptoAsset === undefined || cryptoAsset === null) {
        return "";
      }
      if (!Object.hasOwn(cryptoAsset, "bom-ref")) {
        return "";
      }
      return cryptoAsset["bom-ref"];
    },
  },
  beforeMount() {
    // Executed on page load
    let dependencies = getDependencies(this.bomRef);
    this.dependsOn = dependencies["dependsComponentList"]
    this.provides = dependencies["providesComponentList"]
  },
  computed: {
    dependsOn() {
      return getDependencies(this.bomRef)["dependsComponentList"];
    },
    provides() {
      return getDependencies(this.bomRef)["providesComponentList"];
    }
  }
};
</script>

<style>
title {
  font-weight: 500;
  padding-top: 16px;
  padding-bottom: 4px
}
</style>