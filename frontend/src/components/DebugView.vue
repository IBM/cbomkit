<template>
  <div>
    <div v-if="model.showDebugging">
      <h3>Debugging</h3>
      <pre>{{ JSON.stringify(filteredModel, null, 4) }}</pre>
      <!-- <button @click="model.toggleHomeResultsView()">Click me</button> -->
      <div v-for="(methodName, index) in methodNames" :key="methodName">
        <button @click="model[methodName]()" :key="index">
          {{ methodName }}
        </button>
      </div>
    </div>
    <button
      @click="model.showDebugging = !model.showDebugging"
      style="margin-bottom: 15px"
    >
      Debug
    </button>
  </div>
</template>

<script>
import { model } from "@/model.js";

export default {
  data() {
    return {
      model,
    };
  },
  computed: {
    methodNames() {
      return Object.keys(model).filter(
        (key) => typeof model[key] === "function" && model[key].length === 0
      );
    },
    filteredModel() {
      const { cbom, scanning, lastCboms, ...rest } = this.model;
      if (cbom) {
        let cbomText = JSON.stringify(cbom);
        let addText = cbomText.length > 35 ? "..." : "";
        rest.cbom = cbomText.slice(0, 32) + addText;
      }
      if (lastCboms) {
        rest.lastCboms = `${lastCboms.length} CBOMs`;
      }
      if (scanning) {
        const { liveDetections, ...scanningRest } = scanning;
        if (liveDetections) {
          scanningRest.liveDetections = `${liveDetections.length} detections`;
        }
        rest.scanning = scanningRest;
      }
      return rest;
    },
  },
};
</script>
