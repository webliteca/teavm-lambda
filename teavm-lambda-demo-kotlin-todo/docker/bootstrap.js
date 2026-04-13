const app = require('./teavm-app.js');
const AWSLambda = require('aws-lambda-js-runtime');

const runtime = new AWSLambda.Runtime('http');
const adapter = app.ca_weblite_teavmlambda_adapter_lambda_LambdaAdapter.getInstance();

runtime.start(async (event) => {
    return adapter.handle(event);
});
