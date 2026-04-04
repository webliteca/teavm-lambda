// Bootstrap wrapper for Cloud Run: loads the TeaVM module and calls main()
// which starts the HTTP server via CloudRunAdapter.
const teavm = require('./teavm-app.js');
teavm.main([]);
