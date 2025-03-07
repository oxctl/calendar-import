const config = {
  projects: [
    {
      name: 'assertVariables',
      testMatch: /assertVariables.js/
    },
    // Setup project
    {
      name: 'setup',
      testMatch: /.*\.setup\.js/,
      dependencies: ['assertVariables']
    },
    {
      name: 'chromium',
      use: {
        // Use prepared auth state.
        storageState: 'user.json',
      },
      dependencies: [ 'assertVariables', 'setup' ]
    }
  ],
  timeout: 60000,
  reporter: [ ['html', { open: 'never' }] ],
  retries: 2,
  use: {
    // useful for debugging locally
    // headless: false,
    // viewport: { width: 1280, height: 720 },
    // ignoreHTTPSErrors: true,
    video: 'on',
    launchOptions: {
      slowMo: 500
    }
  },
};

module.exports = config;
