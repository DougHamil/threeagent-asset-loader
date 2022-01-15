process.env.CHROME_BIN = require('puppeteer').executablePath()

module.exports = function(config) {
  config.set({
    browsers: ['ChromeHeadless'],
    basePath: './public_test',
    files: ['ci.js',
            {
              pattern: "./assets/**",
              included: false,
              served: true,
              watched: false,
              nocache: false
            }],
    frameworks: ['cljs-test'],
    plugins: ['karma-cljs-test', 'karma-chrome-launcher'],
    colors: true,
    logLevel: config.LOG_INFO,
    junitReporter: {
      outputDir: 'reports/karma'
    },
    client: {
      args: ["shadow.test.karma.init"],
      singleRun: true
    },
    proxies: {
      "/assets/": "/base/assets/"
    }
  });
}
