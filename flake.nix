{
  description = "distage-example — cross-built distage app with a Scala.js in-browser simulation";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
        # GraalVM CE — used as a regular JDK for `sbt compile/test/run` and
        # additionally provides the `native-image` binary so the local
        # native-image workflow described in the README works without Docker.
        # (The Docker-based `graalVMNativeImageGraalVersion` path in build.sbt
        # still pulls its own image and doesn't depend on this JDK.)
        jdk = pkgs.graalvmPackages.graalvm-ce;
      in {
        devShells.default = pkgs.mkShell {
          name = "distage-example";

          # Build/dev tooling. `nodejs` is here so the linked Scala.js bundle
          # can be exercised outside the browser if needed (none of the demo
          # paths require it — the http4s server serves the UI directly).
          packages = with pkgs; [
            jdk
            sbt
            scala_3
            nodejs_22
            git
          ];

          # sbt sometimes needs more heap when assembling the cross-built
          # project + linking the Scala.js bundle.
          SBT_OPTS = "-Xmx4G -Xss4m";

          JAVA_HOME = "${jdk}/lib/openjdk";

          shellHook = ''
            echo "distage-example dev shell"
            echo "  java     : $(java  -version 2>&1 | head -n1)"
            echo "  sbt      : $(sbt   --script-version 2>/dev/null || sbt --version 2>&1 | tail -n1)"
            echo "  scala 3  : $(scala -version 2>&1 | head -n1)"
            echo
            echo "Quick start:"
            echo "  ./launch-sim                              # build JS, run dummy server, UI at http://localhost:8080/"
            echo "  ./launcher -u scene:managed :leaderboard  # real backend with dockerized postgres"
          '';
        };
      });
}
