# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ClojureScript library for loading ThreeJS assets (models, textures, audio, fonts) into an atom for use with threeagent applications. Assets are defined as a tree structure that mirrors the directory layout on disk.

## Build and Test Commands

```bash
# Install dependencies
npm ci

# Run tests (compile and execute in one step)
npm run test-once

# Or separately:
npm run build-test    # Compile CLJS for karma
npm run test          # Run karma tests

# Watch mode for development
npm run watch-test
```

Tests run via Karma with shadow-cljs. Test files are in `test/` and must have `-test$` suffix.

## Writing Tests

This library supports loading assets by fetching from URLs or reading from a zip file (load and load-zip). All loader implementations should test both types of loading.

## Architecture

### Core Loading System (`src/threeagent/assets/impl/core.cljs`)

The asset tree is processed by a multimethod `visit` that distinguishes:
- **Branch nodes**: directories with optional config (`:loader`, `:middleware`)
- **Leaf nodes**: actual assets identified by `[path key config]` where `key` is a keyword

The loader:
1. Validates for duplicate keys and circular references
2. Resolves asset references (created via `assets/ref`) before loading dependent assets
3. Applies middleware chain (depth-first from outer to inner scopes)
4. Stores results in the provided atom keyed by the asset keyword

### Loaders (`src/threeagent/assets/impl/loader/`)

Each loader is a function `(fn [key path config] -> Promise)`:
- `model.cljs` - GLTF/GLB/FBX via ThreeJS loaders, supports `:scale`, `:pool-size`, `:cast-shadow`, `:receive-shadow`
- `texture.cljs` - ThreeJS TextureLoader with `:repeat`, `:rotation`, `:wrap-s`, `:wrap-t`, etc.
- `audio_howler.cljs` - Howler.js Howl instances, config passed directly to Howl constructor
- `font_troika.cljs` - Troika font preloading with `:characters` option

### Object Pooling (`src/threeagent/assets/pool.cljs`)

For models with `:pool-size`, creates clones using `SkeletonUtils`. Pool is an atom of `{uuid -> Object3D}`. Use `claim!`/`return!` to manage instances.

### Asset References

Use `(assets/ref :other-asset-key)` in config to create dependencies. The referenced asset loads first and its value is substituted into the config before the dependent asset loads.
