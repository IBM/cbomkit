import { model } from "@/model.js";

export function getTitle() {
  return String(process.env.VUE_APP_TITLE || 'CBOM Service');
}

export function isViewerOnly() {
  return String(process.env.VUE_APP_VIEWER_ONLY) === "true";
}

export function capitalizeFirstLetter(string) {
  return string.charAt(0).toUpperCase() + string.slice(1);
}

// Format a big number using K for thousands and M for millions
export function numberFormatter(num) {
  if (Math.abs(num) > 999) {
    if (Math.abs(num) > 999999) {
      return Math.sign(num) * (Math.abs(num) / 1000000).toFixed(1) + "M";
    } else {
      return Math.sign(num) * (Math.abs(num) / 1000).toFixed(1) + "K";
    }
  } else {
    return Math.sign(num) * Math.abs(num);
  }
}

// Format a number in miliseconds to a human readable format like '7m 32s'
export function formatSeconds(seconds) {
  if (seconds < 0) {
    return "Invalid input";
  }

  // Calculate seconds and minutes
  let minutes = Math.floor(seconds / 60);

  // Format the result
  if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`;
  } else {
    // Prevent displaying a time of 0s
    if (seconds === 0) {
      return "1s";
    }
    return `${seconds}s`;
  }
}

export function openGitRepo(gitUrl) {
  window.open(gitUrl, "_blank", "noreferrer");
}

export function canOpenOnline() {
  let gitUrl = model.codeOrigin.gitLink;
  let branch = model.codeOrigin.gitBranch;

  if (gitUrl === undefined || gitUrl === null) {
    return false;
  }

  if (branch === undefined || branch === null) {
    return false;
  }

  return true;
}

export function openOnline(component) {
  if (!canOpenOnline()) {
    return;
  }

  let gitUrl = model.codeOrigin.gitLink;
  let branch = model.codeOrigin.gitBranch;

  const occurrences = component.evidence.occurrences;
  if (occurrences.length === 1) {
    const firstEntry = occurrences[0];
    const filePath = firstEntry.location;
    const lineNumber = firstEntry.line;
    var codeUrl;
    if (gitUrl.includes("github.com") || gitUrl.includes("gitlab.com")) {
      codeUrl = gitUrl + "/blob/" + branch + "/" + filePath + "#L" + lineNumber;
    } else if (gitUrl.includes("bitbucket.org")) {
      codeUrl =
        gitUrl + "/blob/" + branch + "/" + filePath + "#lines-" + lineNumber;
    }
    window.open(codeUrl, "_blank", "noreferrer");
  } else {
    console.error(
      "More than one context was found in the cryptoProperties of the component"
    );
    model.addError(""); // Displays an unknown error
  }
}
