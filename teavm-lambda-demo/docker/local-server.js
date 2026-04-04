/**
 * Local development server that wraps the TeaVM-compiled Lambda handler
 * in a Node.js HTTP server. Translates HTTP requests to API Gateway v1
 * proxy events and responses back to HTTP.
 *
 * Usage: node local-server.js
 * Env:   PORT (default 3000), DATABASE_URL
 */
const http = require('http');
const url = require('url');

// Load the TeaVM-compiled module
const app = require('./index.js');

// TeaVM exports main() - call it to initialize the app (creates router, registers handler)
if (typeof app.main === 'function') {
    app.main([]);
}

const PORT = parseInt(process.env.PORT || '3000', 10);

const server = http.createServer(async (req, res) => {
    // Collect request body
    const body = await new Promise((resolve) => {
        const chunks = [];
        req.on('data', (chunk) => chunks.push(chunk));
        req.on('end', () => resolve(chunks.length > 0 ? Buffer.concat(chunks).toString() : null));
    });

    const parsedUrl = url.parse(req.url, true);

    // Build API Gateway v1 proxy event
    const event = {
        httpMethod: req.method,
        path: parsedUrl.pathname,
        headers: req.headers,
        queryStringParameters: Object.keys(parsedUrl.query).length > 0 ? parsedUrl.query : null,
        body: body,
        isBase64Encoded: false,
        requestContext: {
            httpMethod: req.method,
            path: parsedUrl.pathname,
        },
    };

    const context = {
        functionName: 'teavm-lambda-demo',
        functionVersion: '$LATEST',
        memoryLimitInMB: '256',
        getRemainingTimeInMillis: () => 30000,
    };

    try {
        const response = await app.handler(event, context);

        // Set response headers
        if (response.headers) {
            for (const [key, value] of Object.entries(response.headers)) {
                res.setHeader(key, value);
            }
        }

        res.writeHead(response.statusCode || 200);
        res.end(response.body || '');
    } catch (err) {
        console.error('Handler error:', err);
        res.writeHead(500);
        res.end(JSON.stringify({ error: 'Internal Server Error', message: err.message }));
    }
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`Local Lambda server running at http://0.0.0.0:${PORT}`);
    console.log(`DATABASE_URL: ${process.env.DATABASE_URL || '(not set)'}`);
});
