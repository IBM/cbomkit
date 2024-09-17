<template>
  <!-- The div below controls the light or dark mode using classes -->
  <div
    :class="{
      'make-the-carbon-theme-go-dark': model.useDarkMode,
      'make-the-carbon-theme-go-white': !model.useDarkMode,
    }"
  >
    <div class="main">
      <!-- PRODUCTION APP -->
      <HeaderBar />
      <div style="padding: 60px 4% 1%" />
      <NotificationsView />
      <Transition :name="transitionName" mode="out-in">
        <ResultsView v-if="model.showResults" />
        <HomeView v-else />
      </Transition>
      <!-- DEVELOPMENT APP (with debugging options) -->
      <!-- <div class="container" v-if="true">
        <div class="left">
          <HeaderBar/>
          <div style="padding: 60px 4% 1%;"/>
          <NotificationsView/>
          <Transition :name="transitionName" mode="out-in">
            <ResultsView v-if="model.showResults"/>
            <HomeView v-else/>
          </Transition>
        </div>
        <div :style="{flex: model.showDebugging ? 1 : 0}">
          <DebugView style="padding: 50px 0px 0px;" v-if="true"/>
        </div>
      </div> -->
    </div>
    <FooterView />
  </div>
</template>

<script>
import { model } from "@/model.js";
import HeaderBar from "@/components/global/HeaderBar.vue";
import HomeView from "@/components/home/HomeView.vue";
import ResultsView from "@/components/results/ResultsView.vue";
import NotificationsView from "@/components/global/NotificationsView.vue";
import FooterView from "@/components/global/FooterView.vue";
import DebugView from "@/components/DebugView.vue";
import "@/styles/carbon-both.css"; // This stylesheet contains both light and dark modes

export default {
  name: "App",
  components: {
    HeaderBar,
    HomeView,
    ResultsView,
    NotificationsView,
    FooterView,
    // eslint-disable-next-line vue/no-unused-components
    DebugView,
  },
  data() {
    return {
      model,
      isDarkMode: window.matchMedia("(prefers-color-scheme: dark)").matches,
    };
  },
  computed: {
    transitionName() {
      return this.model.showResults ? "slide-results" : "slide-home";
    },
  },
  watch: {
    "model.useDarkMode": function (newMode) {
      // The classes 'make-the-carbon-theme-go-dark' and 'make-the-carbon-theme-go-white' control the theme of the components
      // However, the background of the body and the font color are not affected, and are handled separately here
      document.body.style.color = newMode ? "white" : "black";
      document.body.style.backgroundColor = newMode ? "#262626" : "white";
    },
  },
};
</script>

<style scoped>
.main {
  max-width: 1200px;
  min-height: 100vh; /* Set main height to 100% of the viewport height */
  margin: auto;
}
/* For the debugging view */
.container {
  display: flex;
}
.left {
  flex: 3;
}
/* Transitions */
.slide-results-enter-active,
.slide-results-leave-active {
  transition: opacity 0.25s, transform 0.25s;
}
.slide-results-enter {
  opacity: 0;
  transform: translateX(60px);
}
.slide-results-leave-to {
  opacity: 0;
  transform: translateX(-60px);
}

.slide-home-enter-active,
.slide-home-leave-active {
  transition: opacity 0.25s, transform 0.25s;
}
.slide-home-enter {
  opacity: 0;
  transform: translateX(-60px);
}
.slide-home-leave-to {
  opacity: 0;
  transform: translateX(60px);
}
</style>
