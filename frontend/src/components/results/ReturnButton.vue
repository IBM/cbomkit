<template>
  <div>
    <cv-button :icon="Rotate24" kind="ghost" @click="onClick()">
      Start over
    </cv-button>
  </div>
</template>

<script>
import { model } from "@/model.js";
import { stopWebSocket } from "@/helpers.js";
import { Rotate24 } from "@carbon/icons-vue";

export default {
  name: "ReturnButton",
  data() {
    return {
      model,
      Rotate24,
    };
  },
  methods: {
    stopWebSocket,
    onClick: function () {
      stopWebSocket();
      model.startAgain();
    },
  },
};
</script>
