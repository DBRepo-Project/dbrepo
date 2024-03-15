# User Interface

* Runtime: Node.js 18+ or [Bun](https://bun.sh/) (recommended)
* Package manager: `bun`
* Builder: `vite`
* Storage: 🍍 `pinia` (with `pinia-plugin-persistedstate/nuxt` using local storage)
* User config: `nitro`
* Runtime modules: `vue`, `vuetify`

## Prerequisites

Install the runtime dependencies from the `package.json`:

```bash
bun install
```

## Develop

The folder structure follows the Nuxt auto-import configuration, i.e. 
the [`/layouts`](https://nuxt.com/docs/guide/directory-structure/layouts) directory is imported automatically by Nuxt.

Then, start a local development server at port 3001.

```bash
bun dev
```

Visit [http://localhost:3001](http://localhost:3001) in your browser. The development server watches for changes in
`dbrepo-ui` and will reload the frontend.

## Configure

To change most display settings, modify the `dbrepo.config.json` in the root folder. Extend it for any configuration
that the user needs to do, e.g. change the title, logo, display a list of links.

### Locale

By default an English local is installed, you can use it as template to write your own locale translation. It follows a
simple structure:

```json
{
  "pages": {
    "some-page": {
      "subpages": {
        "some-other-page": {
          ...
        }
      },
      ...
    }
  },
  "toolbars": {
    "some-user-toolbar": {
      ...
    }
  },
  "notifications": {
    ...
  }
}
```

## Test

To run the unit tests:

```bash
yarn run test:unit
```

Optionally, generate a coverage report:

```bash
yarn run coverage
```

## Packaging

For production builds that build a compressed Node.js server:

```bash
bun run build
```

*Optional*: preview the production server with `bun run preview`.

Start the production server:

```bash
bun run .output/server/index.mjs
```

## Troubleshooting

1. Running `bun dev` for some reason does not apply `<NuxtLink>` on the root page `/`.

* Cause: Unknown.
* Solution: Refresh the page with <kbd>F5</kbd> or <kbd>Ctrl</kbd>+<kbd>R</kbd>

1. Watchpack Error (watcher): Error: ENOSPC: System limit for number of file watchers
   reached, watch `./dbrepo-ui/node_modules/...`.

* Cause: Started the local development server with `yarn dev` and the system file watchers could not be created since
  the maximum limit is reached, debug with `cat /proc/sys/fs/inotify/max_user_watches`.
* Solution: Increase the limit with `sudo sysctl fs.inotify.max_user_watches=131070` and verify
  success: `sudo sysctl -p`
* See
  further: [https://stackoverflow.com/questions/53930305/nodemon-error-system-limit-for-number-of-file-watchers-reached](https://stackoverflow.com/questions/53930305/nodemon-error-system-limit-for-number-of-file-watchers-reached)
