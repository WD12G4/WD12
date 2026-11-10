package main

import "testing"

// Chemins réels d'exécutables macOS : seules les applications visibles sont retenues.
func TestAppBundleFromExe(t *testing.T) {
	cases := map[string]string{
		"/Applications/Visual Studio Code.app/Contents/MacOS/Electron":        "Visual Studio Code",
		"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome":        "Google Chrome",
		"/System/Applications/Utilities/Terminal.app/Contents/MacOS/Terminal": "Terminal",
		"/Applications/Safari.app/Contents/MacOS/Safari":                      "Safari",
		"/Users/paul/Applications/IntelliJ IDEA.app/Contents/MacOS/idea":      "IntelliJ IDEA",
		"/System/Library/CoreServices/Finder.app/Contents/MacOS/Finder":       "",
		"/System/Library/CoreServices/Dock.app/Contents/MacOS/Dock":           "",
		"/Applications/Google Chrome.app/Contents/Frameworks/Google Chrome Framework.framework/Versions/1/Helpers/Google Chrome Helper (Renderer).app/Contents/MacOS/Google Chrome Helper (Renderer)": "",
		"/Applications/Slack.app/Contents/MacOS/Slack Helper": "",
		"/usr/libexec/trustd": "",
		"/usr/bin/python3":    "",
		"":                    "",
	}
	for exe, want := range cases {
		if got := appBundleFromExe(exe); got != want {
			t.Errorf("appBundleFromExe(%q) = %q, attendu %q", exe, got, want)
		}
	}
}
