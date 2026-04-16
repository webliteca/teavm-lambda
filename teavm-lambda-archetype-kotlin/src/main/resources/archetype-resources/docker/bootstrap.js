const teavm = require('./teavm-app.js');
teavm.main([]);
exports.handler = async (event, context) => {
    return global.__teavmLambdaHandler(event, context);
};
