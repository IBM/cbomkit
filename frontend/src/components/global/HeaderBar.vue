<!-- The HeaderBar contains the button to toggle between auto, light and dark themes -->
<!-- It is also the component responsible to listen to OS theme changes and subsequently update model.useDarkMode -->
<template>
  <cv-header aria-label="Carbon header">
    <cv-header-name
      href="https://research.ibm.com"
      prefix="IBM"
      target="_blank"
    >
      Research
    </cv-header-name>
    <template v-slot:header-global>
      <h4 style="margin: auto 0px auto -25px; color: white">|</h4>
      <span style="margin: auto auto auto 8px; color: white">
        {{ getTitle }}
      </span>
      <cv-header-global-action
        @click="updateTheme"
        :label="tipText"
        tipPosition="bottom"
        tipAlignment="end"
      >
        <BrightnessContrast24 v-if="renderedTheme == 'auto'" />
        <Awake24 v-if="renderedTheme == 'light'" />
        <Moon24 v-if="renderedTheme == 'dark'" />
      </cv-header-global-action>
    </template>
  </cv-header>
</template>

<script>
import { model } from "@/model.js";
import { getTitle } from "@/helpers.js";
import { Awake24, Moon24, BrightnessContrast24 } from "@carbon/icons-vue";

export default {
  name: "HeaderBar",
  data() {
    return {
      model,
      renderedTheme: "auto",
      isDarkModeOS: false,
    };
  },
  components: {
    Awake24,
    Moon24,
    BrightnessContrast24,
  },
  methods: {
    updateTheme: function () {
      if (this.renderedTheme == "auto") {
        this.renderedTheme = "light";
        model.useDarkMode = false;
      } else if (this.renderedTheme == "light") {
        this.renderedTheme = "dark";
        model.useDarkMode = true;
      } else if (this.renderedTheme == "dark") {
        this.renderedTheme = "auto";
        model.useDarkMode = this.isDarkModeOS;
      }
    },
  },
  computed: {
    getTitle,
    tipText() {
      if (this.renderedTheme == "auto") {
        return "System theme";
      } else if (this.renderedTheme == "light") {
        return "Light theme";
      } else if (this.renderedTheme == "dark") {
        return "Dark theme";
      }
      return "";
    },
  },
  mounted() {
    // Listen for changes in dark mode
    const darkModeMediaQuery = window.matchMedia(
      "(prefers-color-scheme: dark)"
    );

    // Executed when the OS theme is changed
    const darkModeChanged = (e) => {
      console.log(`Detected OS theme changed: dark mode is ${e.matches}`);
      this.isDarkModeOS = e.matches;
      if (this.renderedTheme == "auto") {
        model.useDarkMode = e.matches;
      }
    };

    darkModeMediaQuery.addEventListener("change", darkModeChanged);

    // Ensure initial state is set correctly
    this.isDarkModeOS = darkModeMediaQuery.matches;
    model.useDarkMode = darkModeMediaQuery.matches;
  },
};
</script>
