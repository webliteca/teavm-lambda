// Bootstrap wrapper: loads the TeaVM module, calls main() to initialize,
// then re-exports the handler that main() registered on module.exports.
const teavm = require('./teavm-app.js');

// main() calls LambdaAdapter.start() which registers exports.handler
teavm.main([]);

// Re-export everything main() registered
module.exports = teavm;
