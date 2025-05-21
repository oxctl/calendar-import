import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import mkcert from 'vite-plugin-mkcert'
import { NodeGlobalsPolyfillPlugin } from '@esbuild-plugins/node-globals-polyfill'
import { NodeModulesPolyfillPlugin } from '@esbuild-plugins/node-modules-polyfill'
import nodePolyfills from 'rollup-plugin-polyfill-node'

export default defineConfig({
    // This is needed for deploying to GitHub pages where we might
    // not be deployed at the root
    base: './',
    server: {
        https: true,
        port: 3000
    },
    plugins: [
        react(),
        process.env.CI !== 'true' && mkcert()
    ],
    // This is to get rid of errors with Instructure UI which depend on import.meta.env
    define: {
        'import.meta.env': {}
    },
    optimizeDeps: {
        include: ['@oxctl/ui-lti'],
        esbuildOptions: {
            // Node.js global to browser globalThis
            define: {
                global: 'globalThis'
            },
            // Enable esbuild polyfill plugins
            plugins: [
                NodeGlobalsPolyfillPlugin({
                    process: true,
                    buffer: true
                }),
                NodeModulesPolyfillPlugin()
            ]
        }
    },
    build: {
        rollupOptions: {
            plugins: [
                // Enable rollup polyfills plugin
                // used during production bundling
                nodePolyfills()
            ]
        },
        // This means we don't have to change the config in cloudflare.
        outDir: 'build',
        // so we still have the same build directory structure as CRA for legacy reasons
        assetsDir: "static"
    },
    test: {
        globals: true,
        environment: 'jsdom'
    }
})