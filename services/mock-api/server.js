import http from 'node:http';
import { adminOverview, articles, categories, digests } from './data.js';

const port = Number(process.env.PORT ?? 8787);

function sendJson(response, statusCode, payload) {
  response.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type,Authorization'
  });
  response.end(JSON.stringify(payload));
}

function route(request, response) {
  const url = new URL(request.url ?? '/', `http://${request.headers.host}`);

  if (request.method === 'OPTIONS') {
    sendJson(response, 200, { ok: true });
    return;
  }

  if (url.pathname === '/api/categories') {
    sendJson(response, 200, { data: categories });
    return;
  }

  if (url.pathname === '/api/articles/home') {
    sendJson(response, 200, { data: articles });
    return;
  }

  if (url.pathname === '/api/articles') {
    const categoryCode = url.searchParams.get('categoryCode');
    const data = categoryCode ? articles.filter((article) => article.categoryCode === categoryCode) : articles;
    sendJson(response, 200, { data });
    return;
  }

  const articleMatch = url.pathname.match(/^\/api\/articles\/(\d+)$/);
  if (articleMatch) {
    const article = articles.find((item) => item.id === Number(articleMatch[1]));
    sendJson(response, article ? 200 : 404, article ? { data: article } : { message: 'Article not found' });
    return;
  }

  if (url.pathname === '/api/digests/today') {
    sendJson(response, 200, { data: digests });
    return;
  }

  if (url.pathname === '/api/admin/overview') {
    sendJson(response, 200, { data: adminOverview });
    return;
  }

  sendJson(response, 404, { message: 'Not found' });
}

http.createServer(route).listen(port, () => {
  console.log(`PulseBrief mock API listening on http://127.0.0.1:${port}`);
});
