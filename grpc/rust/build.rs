use std::env;
use std::path::PathBuf;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // The proto file is at ../../src/main/proto/freeplane.proto relative to this file
    let proto_path = PathBuf::from(env::var("CARGO_MANIFEST_DIR")?)
        .join("../../src/main/proto/freeplane.proto");

    let proto_dir = PathBuf::from(env::var("CARGO_MANIFEST_DIR")?)
        .join("../../src/main/proto");

    println!("cargo:rerun-if-changed={}", proto_path.display());

    tonic_build::configure()
        .build_server(false)
        .compile_protos(&[proto_path.as_path()], &[proto_dir.as_path()])?;

    Ok(())
}
