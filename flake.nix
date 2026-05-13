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
        # Pin JDK 17 to match the GraalVM Native Image build (graalvm-ce 22.3
        # is JDK 17-based) and the sbtscala/scala-sbt image used in CI/dev.
        jdk = pkgs.temurin-bin-17;
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
