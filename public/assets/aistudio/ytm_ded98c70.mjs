import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { spawn } from 'node:child_process';
import crypto from 'node:crypto';
import readline from 'node:readline/promises';
import { stdin as input, stdout as output } from 'node:process';
import { fileURLToPath } from 'node:url';

const BASE_URL = 'https://music.youtube.com';

const FILTER_PARAMS = {
  songs: 'EgWKAQIIAWoSEAUQAxAJEAoQERAQEAQQDhAV',
  videos: 'EgWKAQIQAWoSEAUQAxAJEAoQERAQEAQQDhAV',
  albums: 'EgWKAQIYAWoSEAUQAxAJEAoQERAQEAQQDhAV',
  artists: 'EgWKAQIgAWoSEAUQAxAJEAoQERAQEAQQDhAV',
  playlists: 'EgeKAQQoADgBahIQBRADEAkQChAREBAQBBAOEBU%3D',
  community_playlists: 'EgeKAQQoAEABahIQBRADEAkQChAREBAQBBAOEBU%3D'
};

let cachedContext = null;
let lastContextTime = 0;

export function parseCookies(input = null) {
  if (input === false || input === '') {
    return '';
  }
  if (input && typeof input === 'string' && input.includes('=') && !existsSync(input)) {
    return input.trim();
  }
  if (input && typeof input === 'string' && existsSync(input)) {
    try {
      return readFileSync(input, 'utf8')
        .split(/\r?\n/)
        .map(line => line.trim())
        .filter(line => line && (!line.startsWith('#') || line.startsWith('#HttpOnly_')))
        .map(line => {
          const clean = line.replace(/^#HttpOnly_/, '');
          const parts = clean.includes('\t') ? clean.split('\t') : clean.split(/\s+/);
          return parts.length >= 7 ? `${parts[5]}=${parts[6]}` : line;
        })
        .join('; ');
    } catch {
      return '';
    }
  }

  let ytMusicPath = '';
  let ytCookPath = '';
  try {
    ytMusicPath = fileURLToPath(new URL('ytmusic.txt', import.meta.url));
    ytCookPath = fileURLToPath(new URL('ytcook.txt', import.meta.url));
  } catch {}

  const target = (ytMusicPath && existsSync(ytMusicPath))
    ? ytMusicPath
    : existsSync('ytmusic.txt')
    ? 'ytmusic.txt'
    : (ytCookPath && existsSync(ytCookPath))
    ? ytCookPath
    : existsSync('ytcook.txt')
    ? 'ytcook.txt'
    : '';
  if (!target) return '';
  try {
    return readFileSync(target, 'utf8')
      .split(/\r?\n/)
      .map(line => line.trim())
      .filter(line => line && (!line.startsWith('#') || line.startsWith('#HttpOnly_')))
      .map(line => {
        const clean = line.replace(/^#HttpOnly_/, '');
        const parts = clean.includes('\t') ? clean.split('\t') : clean.split(/\s+/);
        return parts.length >= 7 ? `${parts[5]}=${parts[6]}` : line;
      })
      .join('; ');
  } catch {
    return '';
  }
}

function generateSapisidHash(cookieHeader) {
  if (!cookieHeader) return null;
  const match = cookieHeader.match(/(?:^|;\s*)(?:__Secure-3PAPISID|SAPISID|__Secure-1PAPISID)=([^;]+)/);
  if (!match) return null;
  const sapisid = match[1].trim();
  const timestamp = Math.floor(Date.now() / 1000);
  const hash = crypto.createHash('sha1').update(`${timestamp} ${sapisid} ${BASE_URL}`).digest('hex');
  return `SAPISIDHASH ${timestamp}_${hash}`;
}

export async function getInnertubeConfig(customCookie = null) {
  const cookieHeader = parseCookies(customCookie);
  const now = Date.now();
  if (cachedContext && (now - lastContextTime < 1800000) && cachedContext.cookieHeader === cookieHeader) {
    return cachedContext;
  }

  const musicHtml = await fetch(BASE_URL, {
    headers: {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
      'Accept-Language': 'id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7',
      ...(cookieHeader ? { Cookie: cookieHeader } : {})
    }
  }).then(r => r.text());

  const apiKey = musicHtml.match(/"INNERTUBE_API_KEY":\s*"([^"]+)"/)?.[1];
  const clientVersion = musicHtml.match(/"INNERTUBE_CLIENT_VERSION":\s*"([^"]+)"/)?.[1] || '1.20260101.01.00';
  const visitorData = musicHtml.match(/"VISITOR_DATA":\s*"([^"]+)"/)?.[1];

  cachedContext = {
    apiKey,
    clientVersion,
    visitorData,
    cookieHeader,
    context: {
      client: {
        clientName: 'WEB_REMIX',
        clientVersion,
        hl: 'id',
        gl: 'ID',
        ...(visitorData ? { visitorData } : {})
      }
    }
  };
  lastContextTime = now;
  return cachedContext;
}

export function extractVideoId(input) {
  if (!input) return null;
  const str = String(input).trim();
  if (str.startsWith('MPREb_') || str.startsWith('UC') || str.startsWith('VL') || str.startsWith('PL')) {
    return null;
  }
  const urlMatch = str.match(/(?:youtu\.be\/|music\.youtube\.com\/(?:watch\?v=|watch\?.+&v=)|youtube\.com\/(?:embed\/|v\/|watch\?v=|watch\?.+&v=|shorts\/))([a-zA-Z0-9_-]{11})/);
  if (urlMatch) return urlMatch[1];
  if (/^[a-zA-Z0-9_-]{11}$/.test(str)) return str;
  return null;
}

export function extractBrowseId(input) {
  if (!input) return null;
  const str = String(input).trim();
  const channelMatch = str.match(/(?:channel\/|browse\/|artist\/)(UC[a-zA-Z0-9_-]+|FEmusic_[a-zA-Z0-9_-]+)/);
  if (channelMatch) return channelMatch[1];
  const playlistMatch = str.match(/[?&]list=([a-zA-Z0-9_-]+)/);
  if (playlistMatch) {
    const listId = playlistMatch[1];
    return listId.startsWith('VL') ? listId : `VL${listId}`;
  }
  return str;
}

async function apiRequest(endpoint, payload = {}, options = {}) {
  const cookieOpt = options.cookie !== undefined ? options.cookie : options.cookies;
  const cfg = await getInnertubeConfig(cookieOpt);
  const url = `${BASE_URL}/youtubei/v1/${endpoint}?prettyPrint=false${cfg.apiKey ? `&key=${cfg.apiKey}` : ''}`;

  const useAndroidClient = Boolean(options.androidClient);

  const clientContext = useAndroidClient
    ? {
        clientName: 'ANDROID_MUSIC',
        clientVersion: '7.27.52',
        androidSdkVersion: 34,
        osName: 'Android',
        osVersion: '14',
        hl: options.hl || cfg.context.client.hl || 'id',
        gl: options.gl || cfg.context.client.gl || 'ID',
        ...(cfg.visitorData ? { visitorData: cfg.visitorData } : {})
      }
    : {
        clientName: 'WEB_REMIX',
        clientVersion: cfg.clientVersion,
        hl: options.hl || cfg.context.client.hl || 'id',
        gl: options.gl || cfg.context.client.gl || 'ID',
        ...(cfg.visitorData ? { visitorData: cfg.visitorData } : {})
      };

  const bodyData = options.ctoken
    ? {
        context: { client: clientContext },
        continuation: options.ctoken
      }
    : {
        context: { client: clientContext },
        ...payload
      };

  const authHeader = generateSapisidHash(cfg.cookieHeader);

  const headers = {
    'Content-Type': 'application/json',
    'Origin': BASE_URL,
    'Referer': `${BASE_URL}/`,
    'User-Agent': useAndroidClient
      ? 'com.google.android.apps.youtube.music/7.27.52 (Linux; U; Android 14; id_ID) gzip'
      : 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'X-YouTube-Client-Name': useAndroidClient ? '21' : '67',
    'X-YouTube-Client-Version': useAndroidClient ? '7.27.52' : cfg.clientVersion,
    ...(cfg.visitorData ? { 'X-Goog-Visitor-Id': cfg.visitorData } : {}),
    ...(cfg.cookieHeader ? { Cookie: cfg.cookieHeader } : {}),
    ...(authHeader ? { Authorization: authHeader, 'X-Origin': BASE_URL, 'X-Goog-AuthUser': '0' } : {})
  };

  const res = await fetch(url, {
    method: 'POST',
    headers,
    body: JSON.stringify(bodyData)
  });

  if (!res.ok) {
    if (res.status === 400 && cfg.cookieHeader && !options._retriedGuest) {
      return apiRequest(endpoint, payload, { ...options, cookie: false, _retriedGuest: true });
    }
    const errDetail = await res.text().catch(() => '');
    let msg = `YouTube Music API error: HTTP ${res.status}`;
    try {
      const parsedErr = JSON.parse(errDetail);
      if (parsedErr?.error?.message) msg += ` (${parsedErr.error.message})`;
    } catch {}
    throw new Error(msg);
  }
  return res.json();
}

function parseFlexColumn(item, index) {
  return item?.flexColumns?.[index]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs || [];
}

function parseResponsiveItem(item) {
  const ir = item.musicResponsiveListItemRenderer;
  if (!ir) return null;

  const col0Runs = parseFlexColumn(ir, 0);
  const col1Runs = parseFlexColumn(ir, 1);
  const col2Runs = parseFlexColumn(ir, 2);

  const title = col0Runs[0]?.text || '';
  const firstCol1 = col1Runs[0]?.text || '';
  const fullSubtitle = col1Runs.map(r => r.text).join('');

  const artists = col1Runs
    .filter(r => r.navigationEndpoint?.browseEndpoint?.browseId?.startsWith('UC'))
    .map(r => ({
      name: r.text,
      id: r.navigationEndpoint?.browseEndpoint?.browseId
    }));

  const albumRun = col1Runs.find(r => r.navigationEndpoint?.browseEndpoint?.browseId?.startsWith('MPREb_')) ||
                   col2Runs.find(r => r.navigationEndpoint?.browseEndpoint?.browseId?.startsWith('MPREb_'));
  const album = albumRun ? {
    name: albumRun.text,
    id: albumRun.navigationEndpoint?.browseEndpoint?.browseId
  } : null;

  const durationRun = col1Runs.find(r => /^\d+:\d+$/.test(r.text)) ||
                      col2Runs.find(r => /^\d+:\d+$/.test(r.text)) ||
                      ir.fixedColumns?.[0]?.musicResponsiveListItemFixedColumnRenderer?.text?.runs?.[0];
  const duration = durationRun?.text || null;

  const playsRun = col1Runs.find(r => /plays|views|diputar|pemutaran|tayangan|x ditonton/i.test(r.text)) ||
                   col2Runs.find(r => /plays|views|diputar|pemutaran|tayangan|x ditonton/i.test(r.text));
  const plays = playsRun?.text || null;

  const videoId = ir.playlistItemData?.videoId ||
                  ir.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId ||
                  col0Runs[0]?.navigationEndpoint?.watchEndpoint?.videoId;

  const browseId = col0Runs[0]?.navigationEndpoint?.browseEndpoint?.browseId ||
                   ir.navigationEndpoint?.browseEndpoint?.browseId;

  const thumbnails = ir.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails || [];
  const thumbnail = thumbnails.length ? thumbnails[thumbnails.length - 1].url : null;

  let itemType = 'unknown';
  const lowCol1 = firstCol1.toLowerCase();
  if (lowCol1.includes('song') || lowCol1.includes('lagu')) itemType = 'song';
  else if (lowCol1.includes('video')) itemType = 'video';
  else if (lowCol1.includes('artist') || lowCol1.includes('artis')) itemType = 'artist';
  else if (lowCol1.includes('album') || lowCol1.includes('ep') || lowCol1.includes('single')) itemType = 'album';
  else if (lowCol1.includes('playlist')) itemType = 'playlist';
  else if (videoId) itemType = 'song';
  else if (browseId?.startsWith('UC')) itemType = 'artist';
  else if (browseId?.startsWith('MPREb_')) itemType = 'album';
  else if (browseId?.startsWith('VL')) itemType = 'playlist';

  return {
    type: itemType,
    title,
    subtitle: fullSubtitle || null,
    videoId: videoId || null,
    browseId: browseId || null,
    artists,
    album,
    duration,
    plays,
    thumbnail,
    thumbnails
  };
}

// Parser khusus untuk kartu besar "Rekomendasi harian Anda" (musicMultiRowListItemRenderer)
function parseMultiRowItem(item) {
  const mr = item.musicMultiRowListItemRenderer;
  if (!mr) return null;

  const title = mr.title?.runs?.map(r => r.text).join('') || '';
  const subtitle = mr.subtitle?.runs?.map(r => r.text).join('') || '';
  const secondTitle = mr.secondTitle?.runs?.map(r => r.text).join('') || null;
  const description = mr.description?.runs?.map(r => r.text).join('') || null;

  const videoId = mr.onTap?.watchEndpoint?.videoId ||
                  mr.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId ||
                  mr.title?.runs?.[0]?.navigationEndpoint?.watchEndpoint?.videoId || null;

  const browseId = mr.onTap?.browseEndpoint?.browseId ||
                   mr.title?.runs?.[0]?.navigationEndpoint?.browseEndpoint?.browseId || null;

  const playlistId = mr.onTap?.watchEndpoint?.playlistId ||
                     mr.onTap?.watchPlaylistEndpoint?.playlistId || null;

  const artists = (mr.subtitle?.runs || [])
    .filter(r => r.navigationEndpoint?.browseEndpoint?.browseId?.startsWith('UC'))
    .map(r => ({
      name: r.text,
      id: r.navigationEndpoint?.browseEndpoint?.browseId
    }));

  const playsRun = (mr.subtitle?.runs || []).find(r => /plays|views|diputar|pemutaran|tayangan|x ditonton/i.test(r.text));
  const plays = playsRun?.text || null;

  const thumbs = mr.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails || [];
  const thumbnail = thumbs.length ? thumbs[thumbs.length - 1].url : null;

  return {
    type: videoId ? 'daily_recommendation_track' : 'recommendation_card',
    title,
    subtitle: subtitle || null,
    reason: description || secondTitle || null, // Contoh: "Untuk penggemar Overnight"
    videoId,
    browseId,
    playlistId,
    artists,
    plays,
    thumbnail,
    thumbnails: thumbs
  };
}

function parseHomeSection(s) {
  const carousel = s.musicCarouselShelfRenderer;
  const shelf = s.musicShelfRenderer;
  const tastebuilder = s.musicTastebuilderShelfRenderer;
  const renderer = carousel || shelf || tastebuilder;
  if (!renderer) return null;

  const headerRenderer = carousel?.header?.musicCarouselShelfBasicHeaderRenderer ||
                         shelf?.header?.musicShelfBasicHeaderRenderer;
  const title = headerRenderer?.title?.runs?.map(r => r.text).join('') ||
                shelf?.title?.runs?.map(r => r.text).join('') || null;
  const strapline = headerRenderer?.strapline?.runs?.map(r => r.text).join('') || null;

  const contents = renderer.contents || [];
  const parsedItems = [];

  for (const it of contents) {
    // 1. Kartu Besar seperti "Rekomendasi harian Anda" (musicMultiRowListItemRenderer)
    if (it.musicMultiRowListItemRenderer) {
      const parsedMulti = parseMultiRowItem(it);
      if (parsedMulti) parsedItems.push(parsedMulti);
    }
    // 2. Kartu Standar / Kotak (musicTwoRowItemRenderer)
    else if (it.musicTwoRowItemRenderer) {
      const twoRow = it.musicTwoRowItemRenderer;
      const itTitle = twoRow.title?.runs?.map(r => r.text).join('') || '';
      const itSubtitle = twoRow.subtitle?.runs?.map(r => r.text).join('') || '';
      const bId = twoRow.navigationEndpoint?.browseEndpoint?.browseId || null;
      const vId = twoRow.navigationEndpoint?.watchEndpoint?.videoId ||
                  twoRow.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId || null;
      const playlistId = twoRow.navigationEndpoint?.watchPlaylistEndpoint?.playlistId ||
                         twoRow.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchPlaylistEndpoint?.playlistId || null;
      const thumbs = twoRow.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails || [];

      let itemType = 'playlist';
      if (bId?.startsWith('MPREb_')) itemType = 'album';
      else if (bId?.startsWith('UC')) itemType = 'artist';
      else if (vId) itemType = 'video_or_song';

      parsedItems.push({
        type: itemType,
        title: itTitle,
        subtitle: itSubtitle,
        browseId: bId || (playlistId ? `VL${playlistId.replace(/^VL/, '')}` : null),
        videoId: vId,
        playlistId,
        thumbnail: thumbs.length ? thumbs[thumbs.length - 1].url : null,
        thumbnails: thumbs
      });
    }
    // 3. Baris Lagu seperti "Lagu trending untuk kamu" & "Cover dan remix"
    else if (it.musicResponsiveListItemRenderer) {
      const parsedTrack = parseResponsiveItem(it);
      if (parsedTrack) parsedItems.push(parsedTrack);
    }
  }

  if (!title || parsedItems.length === 0) return null;

  return {
    title,
    ...(strapline ? { strapline } : {}),
    totalItems: parsedItems.length,
    items: parsedItems
  };
}

// Helper untuk membuat "Rekomendasi harian Anda" jika akun belum punya cookie / mode Guest
async function buildFallbackDailyRecommendations(existingSections, reqOpts) {
  try {
    // Cari lagu populer dari section yang ada untuk dijadikan seed rekomendasi harian
    let seedVideoId = null;
    let seedTitle = null;

    for (const sec of existingSections) {
      const foundTrack = sec.items?.find(i => i.videoId);
      if (foundTrack) {
        seedVideoId = foundTrack.videoId;
        seedTitle = foundTrack.artists?.[0]?.name || foundTrack.title;
        break;
      }
    }

    if (!seedVideoId) {
      const sRes = await search('Hits Indonesia Terbaru', { ...reqOpts, filter: 'songs' });
      if (sRes?.[0]?.videoId) {
        seedVideoId = sRes[0].videoId;
        seedTitle = sRes[0].artists?.[0]?.name || sRes[0].title;
      }
    }

    if (!seedVideoId) return null;

    const qData = await getQueue(seedVideoId, reqOpts);
    const tracks = (qData.queue || []).slice(0, 10).map(item => ({
      type: 'daily_recommendation_track',
      title: item.title,
      subtitle: item.subtitle,
      reason: seedTitle ? `Berdasarkan ${seedTitle}` : 'Rekomendasi untuk Anda',
      videoId: item.videoId,
      duration: item.duration,
      thumbnail: item.thumbnail ? item.thumbnail.replace(/=w\d+-h\d+.*$/, '=w800-h800-l90-rj') : null
    }));

    if (!tracks.length) return null;

    return {
      title: 'Rekomendasi harian Anda',
      strapline: 'REKOMENDASI OTOMATIS',
      totalItems: tracks.length,
      items: tracks
    };
  } catch {
    return null;
  }
}

export async function getHome(options = {}) {
  const reqOpts = {
    hl: options.hl || 'id',
    gl: options.gl || 'ID',
    ...options
  };

  const res = await apiRequest('browse', { browseId: 'FEmusic_home' }, reqOpts);
  const sectionList = res.contents?.singleColumnBrowseResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer;

  const rawSections = [...(sectionList?.contents || [])];
  let ctoken = sectionList?.continuations?.[0]?.nextContinuationData?.continuation ||
               sectionList?.continuations?.[0]?.reloadContinuationData?.continuation;

  // Naikkan depth ke 6 agar rak bawah seperti "Rekomendasi harian Anda" & "Cover dan remix" ikut terambil
  const maxContinuations = options.depth !== undefined ? options.depth : 6;
  let count = 0;

  while (ctoken && count < maxContinuations) {
    try {
      const contRes = await apiRequest('browse', {}, { ...reqOpts, ctoken });
      const contSectionList = contRes.continuationContents?.sectionListContinuation ||
                              contRes.onResponseReceivedActions?.[0]?.appendContinuationItemsAction;
      const newItems = contSectionList?.contents || contSectionList?.continuationItems;
      if (newItems) {
        rawSections.push(...newItems);
      }
      ctoken = contSectionList?.continuations?.[0]?.nextContinuationData?.continuation || null;
      count++;
    } catch {
      break;
    }
  }

  const sections = rawSections.map(parseHomeSection).filter(Boolean);

  // Cek apakah "Rekomendasi harian Anda" / "Your daily discover" sudah ada di daftar section
  const hasDailyRec = sections.some(s =>
    /rekomendasi harian|daily discover|pilihan harian/i.test(s.title)
  );

  // Jika belum ada (misal karena buka via Web atau tanpa Cookie), otomatis buatkan & sisipkan
  if (!hasDailyRec && options.includeDaily !== false) {
    const fallbackDaily = await buildFallbackDailyRecommendations(sections, reqOpts);
    if (fallbackDaily) {
      // Sisipkan tepat sebelum "Cover dan remix" (seperti di screenshot aplikasi) atau di urutan ke-3
      const coverIdx = sections.findIndex(s => /cover|remix/i.test(s.title));
      if (coverIdx !== -1) {
        sections.splice(coverIdx, 0, fallbackDaily);
      } else {
        sections.splice(Math.min(2, sections.length), 0, fallbackDaily);
      }
    }
  }

  return {
    language: reqOpts.hl,
    region: reqOpts.gl,
    totalSections: sections.length,
    sections
  };
}

export async function search(query, options = {}) {
  if (!query || !query.trim()) throw new Error('Search query cannot be empty');
  const filter = (options.filter || 'all').toLowerCase();
  const payload = { query: query.trim() };

  if (FILTER_PARAMS[filter]) {
    payload.params = FILTER_PARAMS[filter];
  }

  const data = await apiRequest('search', payload, options);
  const sections = data.contents?.tabbedSearchResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents || [];

  if (filter !== 'all') {
    const shelf = sections.find(s => s.musicShelfRenderer)?.musicShelfRenderer;
    const contents = shelf?.contents || [];
    return contents.map(parseResponsiveItem).filter(Boolean);
  }

  let topResult = null;
  const topCard = sections.find(s => s.musicCardShelfRenderer)?.musicCardShelfRenderer;
  if (topCard) {
    const title = topCard.title?.runs?.[0]?.text;
    const subtitleRuns = topCard.subtitle?.runs || [];
    const fullSubtitle = subtitleRuns.map(r => r.text).join('');
    const videoId = topCard.onTap?.watchEndpoint?.videoId ||
                    topCard.buttons?.[0]?.buttonRenderer?.command?.watchEndpoint?.videoId;
    const browseId = topCard.onTap?.browseEndpoint?.browseId;
    const thumbs = topCard.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails || [];

    topResult = {
      title,
      type: subtitleRuns[0]?.text || 'top',
      subtitle: fullSubtitle,
      videoId: videoId || null,
      browseId: browseId || null,
      thumbnail: thumbs.length ? thumbs[thumbs.length - 1].url : null,
      thumbnails: thumbs
    };
  }

  const songs = [];
  const videos = [];
  const albums = [];
  const artists = [];
  const playlists = [];

  for (const s of sections) {
    if (s.itemSectionRenderer) {
      const items = s.itemSectionRenderer.contents || [];
      for (const it of items) {
        const parsed = parseResponsiveItem(it);
        if (!parsed) continue;
        if (parsed.type === 'song') songs.push(parsed);
        else if (parsed.type === 'video') videos.push(parsed);
        else if (parsed.type === 'album') albums.push(parsed);
        else if (parsed.type === 'artist') artists.push(parsed);
        else if (parsed.type === 'playlist') playlists.push(parsed);
      }
    } else if (s.musicShelfRenderer) {
      const shelfTitle = (s.musicShelfRenderer.title?.runs?.[0]?.text || '').toLowerCase();
      const items = s.musicShelfRenderer.contents || [];
      for (const it of items) {
        const parsed = parseResponsiveItem(it);
        if (!parsed) continue;
        if (shelfTitle.includes('song') || shelfTitle.includes('lagu')) songs.push(parsed);
        else if (shelfTitle.includes('video')) videos.push(parsed);
        else if (shelfTitle.includes('album')) albums.push(parsed);
        else if (shelfTitle.includes('artist') || shelfTitle.includes('artis')) artists.push(parsed);
        else if (shelfTitle.includes('playlist')) playlists.push(parsed);
      }
    }
  }

  return {
    query: query.trim(),
    topResult,
    songs,
    videos,
    albums,
    artists,
    playlists
  };
}

export async function getAudioStream(videoIdOrUrl, options = {}) {
  const cookieOpt = options.cookie !== undefined ? options.cookie : options.cookies;
  let videoId = extractVideoId(videoIdOrUrl);
  let candidates = [];

  if (!videoId) {
    const sRes = await search(videoIdOrUrl, options);
    candidates = [
      sRes.topResult?.videoId,
      ...(sRes.songs || []).map(s => s.videoId),
      ...(sRes.videos || []).map(v => v.videoId)
    ].filter(Boolean);
  } else {
    candidates = [videoId];
  }

  const cfg = await getInnertubeConfig(cookieOpt);

  async function fetchStreamForId(id) {
    try {
      const res = await fetch('https://www.youtube.com/youtubei/v1/player?prettyPrint=false', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15',
          ...(cfg.cookieHeader ? { Cookie: cfg.cookieHeader } : {})
        },
        body: JSON.stringify({
          context: {
            client: {
              clientName: 'VISIONOS',
              clientVersion: '1.02',
              deviceMake: 'Apple',
              deviceModel: 'RealityDevice17,1',
              osName: 'visionOS',
              osVersion: '26.5.23O471',
              visitorData: cfg.visitorData,
              hl: 'en',
              gl: 'US'
            }
          },
          videoId: id
        })
      });

      const data = await res.json();
      if (data.playabilityStatus?.status !== 'OK') return null;

      const afs = data.streamingData?.adaptiveFormats || [];
      const m4aTrack = afs.find(f => f.itag === 140 && f.url);
      const webmTrack = afs.find(f => f.itag === 251 && f.url);
      const bestAudio = m4aTrack || webmTrack || afs.find(f => f.mimeType?.includes('audio') && f.url);
      if (!bestAudio) return null;

      const url = bestAudio.contentLength
        ? `${bestAudio.url}&range=0-${Number(bestAudio.contentLength) - 1}`
        : bestAudio.url;

      return {
        videoId: id,
        itag: bestAudio.itag,
        format: bestAudio.mimeType?.includes('mp4') ? 'm4a (aac)' : 'webm (opus)',
        mimeType: bestAudio.mimeType?.split(';')[0],
        bitrate: bestAudio.bitrate,
        audioQuality: bestAudio.audioQuality,
        contentLength: bestAudio.contentLength ? parseInt(bestAudio.contentLength, 10) : null,
        url
      };
    } catch {
      return null;
    }
  }

  for (const id of candidates) {
    const s = await fetchStreamForId(id);
    if (s) return s;
  }

  if (videoId) {
    try {
      const oembedRes = await fetch(`https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=${videoId}&format=json`);
      if (oembedRes.ok) {
        const oembed = await oembedRes.json();
        const cleanAuthor = (oembed.author_name || '').replace(/\s*-\s*Topic/i, '').trim();
        const query = `${cleanAuthor} ${oembed.title}`.trim();
        const sRes = await search(query, options);
        const altCandidates = [
          sRes.topResult?.videoId,
          ...(sRes.songs || []).map(s => s.videoId),
          ...(sRes.videos || []).map(v => v.videoId)
        ].filter(id => id && id !== videoId);

        for (const altId of altCandidates) {
          const s = await fetchStreamForId(altId);
          if (s) return s;
        }
      }
    } catch {}
  }

  return null;
}

export async function getLyrics(videoIdOrUrl, options = {}) {
  let videoId = extractVideoId(videoIdOrUrl);
  if (!videoId) {
    const sRes = await search(videoIdOrUrl, { ...options, filter: 'songs' });
    const first = Array.isArray(sRes) ? sRes[0] : sRes?.songs?.[0];
    if (first?.videoId) videoId = first.videoId;
  }
  if (!videoId) throw new Error('Invalid YouTube video ID or URL');

  const nextData = await apiRequest('next', { videoId }, options);
  const tabs = nextData.contents?.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs || [];
  const lyricsTab = tabs.find(t => t.tabRenderer?.endpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType === 'MUSIC_PAGE_TYPE_TRACK_LYRICS');

  const browseId = lyricsTab?.tabRenderer?.endpoint?.browseEndpoint?.browseId;
  if (!browseId) {
    return { videoId, lyrics: null, source: null };
  }

  const browseData = await apiRequest('browse', { browseId }, options);
  const shelf = browseData.contents?.sectionListRenderer?.contents?.[0]?.musicDescriptionShelfRenderer;

  const lyricsText = shelf?.description?.runs?.map(r => r.text).join('') || null;
  const source = shelf?.footer?.runs?.map(r => r.text).join('') || null;

  return {
    videoId,
    lyrics: lyricsText,
    source
  };
}

export async function getQueue(videoIdOrUrl, options = {}) {
  let videoId = extractVideoId(videoIdOrUrl);
  if (!videoId) {
    const sRes = await search(videoIdOrUrl, { ...options, filter: 'songs' });
    const first = Array.isArray(sRes) ? sRes[0] : sRes?.songs?.[0];
    if (first?.videoId) videoId = first.videoId;
  }
  if (!videoId) throw new Error('Invalid YouTube video ID or URL');

  const nextData = await apiRequest('next', {
    videoId,
    playlistId: `RDAMVM${videoId}`
  }, options);

  const tab0 = nextData.contents?.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs?.[0];
  const queueList = tab0?.tabRenderer?.content?.musicQueueRenderer?.content?.playlistPanelRenderer?.contents || [];

  const queue = [];
  for (const item of queueList) {
    const ppr = item.playlistPanelVideoRenderer;
    if (!ppr) continue;

    const title = ppr.title?.runs?.[0]?.text;
    const vId = ppr.videoId;
    const artistDesc = ppr.longBylineText?.runs?.map(r => r.text).join('') || '';
    const duration = ppr.lengthText?.runs?.[0]?.text || null;
    const thumbs = ppr.thumbnail?.thumbnails || [];

    queue.push({
      title,
      videoId: vId,
      subtitle: artistDesc,
      duration,
      thumbnail: thumbs.length ? thumbs[thumbs.length - 1].url : null
    });
  }

  return {
    videoId,
    total: queue.length,
    queue
  };
}

export async function getTrack(videoIdOrUrl, options = {}) {
  let videoId = extractVideoId(videoIdOrUrl);
  if (!videoId) {
    const sRes = await search(videoIdOrUrl, { ...options, filter: 'songs' });
    const first = Array.isArray(sRes) ? sRes[0] : sRes?.songs?.[0];
    if (first?.videoId) videoId = first.videoId;
  }
  if (!videoId) throw new Error('Invalid YouTube video ID or URL');

  const [playerData, audio] = await Promise.all([
    apiRequest('player', { videoId }, options),
    getAudioStream(videoId, options)
  ]);

  const vd = playerData.videoDetails || {};
  const lengthSeconds = parseInt(vd.lengthSeconds, 10) || 0;
  const mins = Math.floor(lengthSeconds / 60);
  const secs = String(lengthSeconds % 60).padStart(2, '0');
  const durationText = `${mins}:${secs}`;

  const thumbs = vd.thumbnail?.thumbnails || [];
  const thumbnail = thumbs.length ? thumbs[thumbs.length - 1].url : `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`;

  let lyrics = null;
  if (options.lyrics) {
    lyrics = await getLyrics(videoId, options);
  }

  let queue = null;
  if (options.queue || options.related) {
    queue = await getQueue(videoId, options);
  }

  return {
    videoId,
    title: vd.title || null,
    author: vd.author || null,
    channelId: vd.channelId || null,
    durationSeconds: lengthSeconds,
    duration: durationText,
    thumbnail,
    thumbnails: thumbs,
    audio,
    ...(lyrics ? { lyrics: lyrics.lyrics, lyricsSource: lyrics.source } : {}),
    ...(queue ? { queue: queue.queue } : {})
  };
}

export async function getAlbum(browseIdOrUrl, options = {}) {
  const browseId = extractBrowseId(browseIdOrUrl);
  if (!browseId) throw new Error('Invalid album browseId or URL');

  const res = await apiRequest('browse', { browseId }, options);
  const tc = res.contents?.twoColumnBrowseResultsRenderer;

  const tab0Hdr = tc?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents?.[0]?.musicResponsiveHeaderRenderer;
  const title = tab0Hdr?.title?.runs?.[0]?.text;
  const subtitle = tab0Hdr?.subtitle?.runs?.map(r => r.text).join('') || '';
  const secondSubtitle = tab0Hdr?.secondSubtitle?.runs?.map(r => r.text).join('') || '';
  const thumbs = tab0Hdr?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails || [];
  const thumbnail = thumbs.length ? thumbs[thumbs.length - 1].url : null;
  const description = tab0Hdr?.description?.musicDescriptionShelfRenderer?.description?.runs?.map(r => r.text).join('') || null;

  const secContents = tc?.secondaryContents?.sectionListRenderer?.contents || [];
  const shelf = secContents.find(c => c.musicShelfRenderer)?.musicShelfRenderer;
  const trackItems = shelf?.contents || [];

  const tracks = [];
  trackItems.forEach((item, idx) => {
    const ir = item.musicResponsiveListItemRenderer;
    if (!ir) return;
    const trackTitle = ir.flexColumns?.[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.text;
    const duration = ir.fixedColumns?.[0]?.musicResponsiveListItemFixedColumnRenderer?.text?.runs?.[0]?.text || null;
    const videoId = ir.playlistItemData?.videoId ||
                    ir.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId;
    const artists = ir.flexColumns?.[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
      ?.filter(r => r.navigationEndpoint?.browseEndpoint?.browseId)
      ?.map(r => ({ name: r.text, id: r.navigationEndpoint?.browseEndpoint?.browseId })) || [];

    tracks.push({
      trackNumber: idx + 1,
      title: trackTitle,
      videoId,
      duration,
      artists
    });
  });

  return {
    browseId,
    title,
    subtitle,
    secondSubtitle,
    thumbnail,
    thumbnails: thumbs,
    description,
    totalTracks: tracks.length,
    tracks
  };
}

export async function getArtist(browseIdOrUrl, options = {}) {
  const browseId = extractBrowseId(browseIdOrUrl);
  if (!browseId) throw new Error('Invalid artist browseId or URL');

  const res = await apiRequest('browse', { browseId }, options);
  const hdr = res.header?.musicImmersiveHeaderRenderer;
  const name = hdr?.title?.runs?.[0]?.text;
  const description = hdr?.description?.runs?.map(r => r.text).join('') || null;
  const thumbs = hdr?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails || [];
  const thumbnail = thumbs.length ? thumbs[thumbs.length - 1].url : null;

  const sections = res.contents?.singleColumnBrowseResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents || [];

  const topSongs = [];
  const albums = [];
  const singles = [];
  const videos = [];
  const similarArtists = [];

  for (const s of sections) {
    if (s.musicShelfRenderer) {
      const items = s.musicShelfRenderer.contents || [];
      items.forEach(it => {
        const parsed = parseResponsiveItem(it);
        if (parsed) topSongs.push(parsed);
      });
    } else if (s.musicCarouselShelfRenderer) {
      const title = (s.musicCarouselShelfRenderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.map(r => r.text).join('') || '').toLowerCase();
      const items = s.musicCarouselShelfRenderer.contents || [];

      items.forEach(it => {
        const twoRow = it.musicTwoRowItemRenderer;
        if (!twoRow) return;

        const itTitle = twoRow.title?.runs?.[0]?.text;
        const itSubtitle = twoRow.subtitle?.runs?.map(r => r.text).join('') || '';
        const bId = twoRow.navigationEndpoint?.browseEndpoint?.browseId;
        const vId = twoRow.navigationEndpoint?.watchEndpoint?.videoId;
        const itThumbs = twoRow.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails || [];

        const obj = {
          title: itTitle,
          subtitle: itSubtitle,
          browseId: bId || null,
          videoId: vId || null,
          thumbnail: itThumbs.length ? itThumbs[itThumbs.length - 1].url : null
        };

        if (title.includes('album')) albums.push(obj);
        else if (title.includes('single') || title.includes('ep') || title.includes('lagu singkat')) singles.push(obj);
        else if (title.includes('video')) videos.push(obj);
        else if (title.includes('fans might') || title.includes('similar') || title.includes('penggemar')) similarArtists.push(obj);
      });
    }
  }

  return {
    browseId,
    name,
    description,
    thumbnail,
    thumbnails: thumbs,
    topSongs,
    albums,
    singles,
    videos,
    similarArtists
  };
}

export async function getPlaylist(browseIdOrUrl, options = {}) {
  const browseId = extractBrowseId(browseIdOrUrl);
  if (!browseId) throw new Error('Invalid playlist browseId or URL');

  const res = await apiRequest('browse', { browseId }, options);
  const tc = res.contents?.twoColumnBrowseResultsRenderer;

  const tabHdr = tc?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents?.[0]?.musicResponsiveHeaderRenderer;
  const title = tabHdr?.title?.runs?.[0]?.text;
  const subtitle = tabHdr?.subtitle?.runs?.map(r => r.text).join('') || '';
  const secondSubtitle = tabHdr?.secondSubtitle?.runs?.map(r => r.text).join('') || '';
  const description = tabHdr?.description?.musicDescriptionShelfRenderer?.description?.runs?.map(r => r.text).join('') || null;
  const thumbs = tabHdr?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails || [];
  const thumbnail = thumbs.length ? thumbs[thumbs.length - 1].url : null;

  const rawTracks = tc?.secondaryContents?.sectionListRenderer?.contents?.[0]?.musicPlaylistShelfRenderer?.contents || [];

  const tracks = [];
  rawTracks.forEach((item, idx) => {
    const ir = item.musicResponsiveListItemRenderer;
    if (!ir) return;

    const trackTitle = ir.flexColumns?.[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.text;
    const videoId = ir.playlistItemData?.videoId;
    const artists = ir.flexColumns?.[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
      ?.filter(r => r.navigationEndpoint?.browseEndpoint?.browseId)
      ?.map(r => ({ name: r.text, id: r.navigationEndpoint?.browseEndpoint?.browseId })) || [];
    const duration = ir.fixedColumns?.[0]?.musicResponsiveListItemFixedColumnRenderer?.text?.runs?.[0]?.text || null;
    const itThumbs = ir.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails || [];

    tracks.push({
      position: idx + 1,
      title: trackTitle,
      videoId,
      artists,
      duration,
      thumbnail: itThumbs.length ? itThumbs[itThumbs.length - 1].url : null
    });
  });

  return {
    browseId,
    title,
    subtitle,
    secondSubtitle,
    description,
    thumbnail,
    thumbnails: thumbs,
    totalTracks: tracks.length,
    tracks
  };
}

export async function getCharts(country = 'ID', options = {}) {
  const res = await apiRequest('browse', { browseId: 'FEmusic_charts' }, { ...options, gl: country });
  const sections = res.contents?.singleColumnBrowseResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents || [];

  const topArtists = [];

  sections.forEach(s => {
    const carousel = s.musicCarouselShelfRenderer;
    if (!carousel) return;
    const title = (carousel.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.map(r => r.text).join('') || '').toLowerCase();
    if (!title.includes('artist') && !title.includes('artis')) return;
    const items = carousel.contents || [];

    items.forEach((it, idx) => {
      const twoRow = it.musicTwoRowItemRenderer;
      const ir = it.musicResponsiveListItemRenderer;

      if (twoRow) {
        const itemTitle = twoRow.title?.runs?.[0]?.text;
        const itemSubtitle = twoRow.subtitle?.runs?.map(r => r.text).join('') || '';
        const bId = twoRow.navigationEndpoint?.browseEndpoint?.browseId;
        const thumbs = twoRow.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails || [];

        topArtists.push({
          rank: idx + 1,
          name: itemTitle,
          subscribers: itemSubtitle,
          browseId: bId || null,
          thumbnail: thumbs.length ? thumbs[thumbs.length - 1].url : null
        });
      } else if (ir) {
        const itemTitle = ir.flexColumns?.[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.text;
        const itemSubtitle = ir.flexColumns?.[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.map(r => r.text).join('') || '';
        const bId = ir.navigationEndpoint?.browseEndpoint?.browseId ||
                    ir.flexColumns?.[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.navigationEndpoint?.browseEndpoint?.browseId;
        const thumbs = ir.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails || [];

        topArtists.push({
          rank: idx + 1,
          name: itemTitle,
          subscribers: itemSubtitle,
          browseId: bId || null,
          thumbnail: thumbs.length ? thumbs[thumbs.length - 1].url : null
        });
      }
    });
  });

  return {
    country,
    topArtists
  };
}

export async function getExplore(options = {}) {
  const res = await apiRequest('browse', { browseId: 'FEmusic_explore' }, options);
  const sections = res.contents?.singleColumnBrowseResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents || [];

  const categories = [];
  sections.forEach(s => {
    const carousel = s.musicCarouselShelfRenderer;
    if (!carousel) return;
    const title = carousel.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.map(r => r.text).join('');
    const items = carousel.contents || [];

    const parsedItems = [];
    items.forEach(it => {
      const twoRow = it.musicTwoRowItemRenderer;
      if (twoRow) {
        const itTitle = twoRow.title?.runs?.[0]?.text;
        const itSubtitle = twoRow.subtitle?.runs?.map(r => r.text).join('') || '';
        const bId = twoRow.navigationEndpoint?.browseEndpoint?.browseId;
        const vId = twoRow.navigationEndpoint?.watchEndpoint?.videoId;
        const thumbs = twoRow.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails || [];

        parsedItems.push({
          title: itTitle,
          subtitle: itSubtitle,
          browseId: bId || null,
          videoId: vId || null,
          thumbnail: thumbs.length ? thumbs[thumbs.length - 1].url : null
        });
      }
    });

    if (title && parsedItems.length) {
      categories.push({
        title,
        items: parsedItems
      });
    }
  });

  return {
    totalCategories: categories.length,
    categories
  };
}

function convertToMp3(inputBuffer) {
  return new Promise((resolve, reject) => {
    const proc = spawn('ffmpeg', [
      '-i', 'pipe:0',
      '-vn',
      '-acodec', 'libmp3lame',
      '-b:a', '192k',
      '-f', 'mp3',
      'pipe:1'
    ], { stdio: ['pipe', 'pipe', 'ignore'] });

    const chunks = [];
    proc.stdout.on('data', chunk => chunks.push(chunk));
    proc.stdout.on('end', () => resolve(Buffer.concat(chunks)));
    proc.on('error', reject);
    proc.stdin.write(inputBuffer);
    proc.stdin.end();
  });
}

export async function download(urlOrId, options = {}) {
  const audio = await getAudioStream(urlOrId, options);
  if (!audio || !audio.url) {
    throw new Error('Failed to resolve audio stream for this track');
  }

  const effectiveId = audio.videoId || extractVideoId(urlOrId);
  const track = await getTrack(effectiveId, options).catch(() => ({}));

  const cleanTitle = (track.title || 'audio').replace(/[/\\?%*:|"<>]/g, '_');
  const cleanAuthor = (track.author || 'YouTube Music').replace(/[/\\?%*:|"<>]/g, '_');
  const wantMp3 = options.format?.toLowerCase() === 'mp3' || Boolean(options.whatsapp);
  const ext = wantMp3 ? 'mp3' : 'm4a';
  const filename = `${cleanAuthor} - ${cleanTitle}.${ext}`;

  const formatSize = (bytes) => {
    if (!bytes) return null;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  };

  let buffer = null;
  let savedPath = null;

  if (options.save || options.outputPath || options.buffer || wantMp3 || options.whatsapp) {
    const res = await fetch(audio.url, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
      }
    });
    if (!res.ok) throw new Error(`Failed to download audio stream: HTTP ${res.status}`);
    const rawBuffer = Buffer.from(await res.arrayBuffer());

    if (wantMp3) {
      buffer = await convertToMp3(rawBuffer);
    } else {
      buffer = rawBuffer;
    }

    if (options.outputPath) {
      savedPath = options.outputPath;
      writeFileSync(savedPath, buffer);
    } else if (options.save) {
      savedPath = path.join('/tmp', filename);
      writeFileSync(savedPath, buffer);
    }
  }

  return {
    videoId: effectiveId,
    title: track.title || cleanTitle,
    artist: track.author || cleanAuthor,
    duration: track.duration || null,
    durationSeconds: track.durationSeconds || null,
    thumbnail: track.thumbnail || `https://i.ytimg.com/vi/${effectiveId}/hqdefault.jpg`,
    filename,
    format: ext,
    mimeType: wantMp3 ? 'audio/mpeg' : audio.mimeType,
    bitrate: audio.bitrate,
    size: buffer ? buffer.length : audio.contentLength,
    formattedSize: formatSize(buffer ? buffer.length : audio.contentLength),
    downloadUrl: audio.url,
    ...(savedPath ? { savedPath } : {}),
    ...(options.buffer || options.whatsapp ? { buffer } : {})
  };
}

export async function getThumbnail(urlOrId, options = {}) {
  const videoId = extractVideoId(urlOrId);
  const browseId = !videoId ? extractBrowseId(urlOrId) : null;

  if (!videoId && !browseId) {
    throw new Error('Invalid YouTube Music video ID, browse ID, or URL');
  }

  let title = '';
  let rawThumbnail = null;
  let allThumbnails = [];

  if (videoId) {
    const track = await getTrack(videoId, options);
    title = track.title || videoId;
    rawThumbnail = track.thumbnail;
    allThumbnails = track.thumbnails || [];
  } else if (browseId.startsWith('MPREb_')) {
    const album = await getAlbum(browseId, options);
    title = album.title || browseId;
    rawThumbnail = album.thumbnail;
    allThumbnails = album.thumbnails || [];
  } else if (browseId.startsWith('UC')) {
    const artist = await getArtist(browseId, options);
    title = artist.name || browseId;
    rawThumbnail = artist.thumbnail;
    allThumbnails = artist.thumbnails || [];
  } else if (browseId.startsWith('VL')) {
    const playlist = await getPlaylist(browseId, options);
    title = playlist.title || browseId;
    rawThumbnail = playlist.thumbnail;
    allThumbnails = playlist.thumbnails || [];
  }

  let ultraHd = rawThumbnail;
  let hd = rawThumbnail;
  let medium = rawThumbnail;
  let small = rawThumbnail;

  if (rawThumbnail && rawThumbnail.includes('googleusercontent.com')) {
    ultraHd = rawThumbnail.replace(/=w\d+-h\d+.*$/, '=w1200-h1200-l90-rj');
    hd = rawThumbnail.replace(/=w\d+-h\d+.*$/, '=w544-h544-l90-rj');
    medium = rawThumbnail.replace(/=w\d+-h\d+.*$/, '=w226-h226-l90-rj');
    small = rawThumbnail.replace(/=w\d+-h\d+.*$/, '=w120-h120-l90-rj');
  } else if (videoId) {
    ultraHd = `https://i.ytimg.com/vi/${videoId}/maxresdefault.jpg`;
    hd = `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`;
    medium = `https://i.ytimg.com/vi/${videoId}/mqdefault.jpg`;
    small = `https://i.ytimg.com/vi/${videoId}/default.jpg`;
  }

  const selectedQuality = (options.quality || 'ultra_hd').toLowerCase();
  const selectedUrl = selectedQuality === 'small' ? small :
                      selectedQuality === 'medium' ? medium :
                      selectedQuality === 'hd' ? hd : ultraHd;

  let buffer = null;
  let savedPath = null;

  if (options.save || options.outputPath || options.buffer) {
    const res = await fetch(selectedUrl, {
      headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' }
    });
    if (!res.ok) throw new Error(`Failed to fetch thumbnail: HTTP ${res.status}`);
    buffer = Buffer.from(await res.arrayBuffer());

    if (options.outputPath) {
      savedPath = options.outputPath;
      writeFileSync(savedPath, buffer);
    } else if (options.save) {
      const cleanName = (title || 'thumbnail').replace(/[/\\?%*:|"<>]/g, '_');
      savedPath = path.join('/tmp', `${cleanName}.jpg`);
      writeFileSync(savedPath, buffer);
    }
  }

  return {
    id: videoId || browseId,
    title,
    quality: selectedQuality,
    url: selectedUrl,
    sizes: {
      ultraHd,
      hd,
      medium,
      small
    },
    allThumbnails,
    ...(savedPath ? { savedPath } : {}),
    ...(options.buffer || options.whatsapp ? { buffer } : {})
  };
}

export default {
  parseCookies,
  getThumbnail,
  download,
  getInnertubeConfig,
  extractVideoId,
  extractBrowseId,
  getHome,
  search,
  getTrack,
  getAudioStream,
  getLyrics,
  getQueue,
  getAlbum,
  getArtist,
  getPlaylist,
  getCharts,
  getExplore
};

// ============================================================================
// BAGIAN INTERAKTIF CLI (Hanya berjalan jika dieksekusi langsung di Terminal)
// ============================================================================

async function displayPaginatedJson(rl, data) {
  let listKey = null;
  let items = null;

  if (Array.isArray(data)) {
    items = data;
  } else if (data && typeof data === 'object') {
    const possibleKeys = ['sections', 'items', 'songs', 'videos', 'tracks', 'queue', 'topArtists', 'categories', 'topSongs', 'albums', 'playlists'];
    for (const k of possibleKeys) {
      if (Array.isArray(data[k]) && data[k].length > 0) {
        listKey = k;
        items = data[k];
        break;
      }
    }
  }

  if (!items || items.length <= 5) {
    console.log('\n=== OUTPUT JSON ===');
    console.log(JSON.stringify(data, null, 2));
    await rl.question('\nTekan [Enter] untuk kembali...');
    return;
  }

  const pageSize = 5;
  const totalPages = Math.ceil(items.length / pageSize);
  let currentPage = 1;

  while (true) {
    console.clear();
    const start = (currentPage - 1) * pageSize;
    const pagedItems = items.slice(start, start + pageSize);

    let displayData;
    if (Array.isArray(data)) {
      displayData = {
        _infoHalaman: `Halaman ${currentPage} dari ${totalPages} (Total: ${items.length} item)`,
        data: pagedItems
      };
    } else {
      displayData = {
        ...data,
        _infoHalaman: `Menampilkan '${listKey}' - Halaman ${currentPage} dari ${totalPages} (Total: ${items.length} item)`,
        [listKey]: pagedItems
      };
    }

    console.log('=== OUTPUT JSON (MODE HALAMAN) ===');
    console.log(JSON.stringify(displayData, null, 2));
    console.log('\n----------------------------------------');
    console.log(`Halaman saat ini: [${currentPage}/${totalPages}]`);
    console.log(`Ketik angka [1-${totalPages}] untuk langsung pindah halaman`);
    console.log(`Ketik [f] untuk tampilkan FULL JSON sekaligus`);
    console.log(`Ketik [0] untuk kembali`);
    console.log('----------------------------------------');

    const ans = (await rl.question('Pilihan Anda: ')).trim().toLowerCase();

    if (ans === '0' || ans === 'q' || ans === 'exit') {
      break;
    } else if (ans === 'f') {
      console.clear();
      console.log('=== FULL OUTPUT JSON ===');
      console.log(JSON.stringify(data, null, 2));
      await rl.question('\nTekan [Enter] untuk kembali ke tampilan halaman...');
    } else {
      const pageNum = parseInt(ans, 10);
      if (!isNaN(pageNum) && pageNum >= 1 && pageNum <= totalPages) {
        currentPage = pageNum;
      } else {
        console.log('Input tidak valid! Masukkan nomor halaman yang sesuai.');
        await new Promise(r => setTimeout(r, 1000));
      }
    }
  }
}

async function runCli() {
  const rl = readline.createInterface({ input, output });
  let customCookie = null;

  while (true) {
    console.clear();
    const activeCookie = parseCookies(customCookie);
    const hasValidAuth = Boolean(generateSapisidHash(activeCookie));
    console.log('==============================================');
    console.log('        YOUTUBE MUSIC JSON CLI (TERMUX)       ');
    console.log('==============================================');
    console.log(`Status Cookie : ${activeCookie ? (hasValidAuth ? 'Aktif (SAPISID Valid)' : 'Terdeteksi (Tanpa SAPISID / Guest Fallback)') : 'Tanpa Cookie (Mode Guest)'}`);
    console.log('----------------------------------------------');
    console.log('Pilih Fitur (Ketik Angka):');
    console.log(' 1. Get Home / Beranda (+ Rekomendasi Harian Anda)');
    console.log(' 2. Search (Cari Lagu, Video, Album, Artis)');
    console.log(' 3. Get Track Info (Detail Lagu + Audio Stream)');
    console.log(' 4. Get Audio Stream (Hanya Data Stream URL)');
    console.log(' 5. Get Lyrics (Lirik Lagu)');
    console.log(' 6. Get Queue / Related (Antrean Lagu Terkait)');
    console.log(' 7. Get Album (Detail & Daftar Lagu Album)');
    console.log(' 8. Get Artist (Profil & Lagu Populer Artis)');
    console.log(' 9. Get Playlist (Detail & Isi Playlist)');
    console.log('10. Get Charts (Tangga Lagu / Top Artis)');
    console.log('11. Get Explore (Eksplorasi Kategori Musik)');
    console.log('12. Get Thumbnail (URL Gambar Berbagai Resolusi)');
    console.log('13. Download Metadata (Info File & URL Tanpa Save)');
    console.log('14. Set / Ganti Cookie (Path File / String / Ketik "off" untuk mematikan)');
    console.log(' 0. Keluar (Exit)');
    console.log('==============================================');

    const choice = (await rl.question('Masukkan nomor menu [0-14]: ')).trim();

    if (choice === '0') {
      console.log('Keluar dari program...');
      rl.close();
      process.exit(0);
    }

    const opts = customCookie !== null ? { cookie: customCookie } : {};

    try {
      let result = null;

      switch (choice) {
        case '1': {
          const hl = (await rl.question('Masukkan kode bahasa (default: id): ')).trim() || 'id';
          const gl = (await rl.question('Masukkan kode negara (default: ID): ')).trim() || 'ID';
          console.log('\nMengambil halaman Home beserta Rekomendasi Harian...');
          const homeData = await getHome({ ...opts, hl, gl, depth: 6 });

          while (true) {
            console.clear();
            console.log(`=== BERANDA YOUTUBE MUSIC (${homeData.language}-${homeData.region}) ===`);
            console.log(`Ditemukan ${homeData.sections.length} Kategori/Section:\n`);

            homeData.sections.forEach((sec, idx) => {
              const sub = sec.strapline ? ` (${sec.strapline})` : '';
              console.log(` ${idx + 1}. ${sec.title}${sub} [${sec.totalItems} item]`);
            });

            console.log('\n----------------------------------------------');
            console.log(`Ketik angka [1-${homeData.sections.length}] untuk melihat JSON per kategori`);
            console.log(`Ketik [f] untuk melihat FULL JSON semua kategori sekaligus`);
            console.log(`Ketik [0] untuk kembali ke Menu Utama`);
            console.log('----------------------------------------------');

            const secChoice = (await rl.question('Pilihan Anda: ')).trim().toLowerCase();
            if (secChoice === '0') break;
            if (secChoice === 'f') {
              await displayPaginatedJson(rl, homeData);
            } else {
              const secIdx = parseInt(secChoice, 10) - 1;
              if (!isNaN(secIdx) && homeData.sections[secIdx]) {
                await displayPaginatedJson(rl, homeData.sections[secIdx]);
              } else {
                console.log('Nomor kategori tidak valid!');
                await new Promise(r => setTimeout(r, 1000));
              }
            }
          }
          result = null;
          break;
        }
        case '2': {
          const q = await rl.question('Masukkan kata kunci pencarian: ');
          console.log('\nPilih Filter Pencarian:');
          console.log('1. Semua (All)   4. Albums');
          console.log('2. Songs         5. Artists');
          console.log('3. Videos        6. Playlists');
          console.log('7. Community Playlists');
          const fChoice = (await rl.question('Pilih nomor filter [1-7, default 1]: ')).trim();
          const filterMap = {
            '1': 'all', '2': 'songs', '3': 'videos',
            '4': 'albums', '5': 'artists', '6': 'playlists', '7': 'community_playlists'
          };
          const filter = filterMap[fChoice] || 'all';
          console.log(`\nSedang mencari "${q}" (Filter: ${filter})...`);
          result = await search(q, { ...opts, filter });
          break;
        }
        case '3': {
          const id = await rl.question('Masukkan Video ID / URL / Judul Lagu: ');
          const incLyrics = (await rl.question('Sertakan lirik? (y/n) [default: n]: ')).trim().toLowerCase() === 'y';
          const incQueue = (await rl.question('Sertakan antrean/related? (y/n) [default: n]: ')).trim().toLowerCase() === 'y';
          console.log('\nMengambil data Track...');
          result = await getTrack(id, { ...opts, lyrics: incLyrics, queue: incQueue });
          break;
        }
        case '4': {
          const id = await rl.question('Masukkan Video ID / URL / Judul Lagu: ');
          console.log('\nMengambil data Audio Stream...');
          result = await getAudioStream(id, opts);
          break;
        }
        case '5': {
          const id = await rl.question('Masukkan Video ID / URL / Judul Lagu: ');
          console.log('\nMengambil Lirik...');
          result = await getLyrics(id, opts);
          break;
        }
        case '6': {
          const id = await rl.question('Masukkan Video ID / URL / Judul Lagu: ');
          console.log('\nMengambil Antrean (Queue)...');
          result = await getQueue(id, opts);
          break;
        }
        case '7': {
          const id = await rl.question('Masukkan Browse ID Album (MPREb_...) atau URL Album: ');
          console.log('\nMengambil data Album...');
          result = await getAlbum(id, opts);
          break;
        }
        case '8': {
          const id = await rl.question('Masukkan Browse ID Artis (UC...) atau URL Channel: ');
          console.log('\nMengambil data Artis...');
          result = await getArtist(id, opts);
          break;
        }
        case '9': {
          const id = await rl.question('Masukkan Browse ID Playlist (VL.../PL...) atau URL: ');
          console.log('\nMengambil data Playlist...');
          result = await getPlaylist(id, opts);
          break;
        }
        case '10': {
          const country = (await rl.question('Masukkan Kode Negara (contoh: ID, US, JP) [default: ID]: ')).trim() || 'ID';
          console.log('\nMengambil data Charts...');
          result = await getCharts(country.toUpperCase(), opts);
          break;
        }
        case '11': {
          console.log('\nMengambil data Explore...');
          result = await getExplore(opts);
          break;
        }
        case '12': {
          const id = await rl.question('Masukkan Video ID / Browse ID / URL: ');
          console.log('\nMengambil data Thumbnail (Tanpa menyimpan ke penyimpanan)...');
          result = await getThumbnail(id, { ...opts, save: false });
          break;
        }
        case '13': {
          const id = await rl.question('Masukkan Video ID / URL / Judul Lagu: ');
          console.log('\nMengambil metadata Download (Tanpa menyimpan file)...');
          result = await download(id, { ...opts, save: false, buffer: false });
          break;
        }
        case '14': {
          const cInput = (await rl.question('Masukkan path file cookie, string cookie, atau ketik "off" untuk mematikan cookie: ')).trim();
          if (cInput.toLowerCase() === 'off') {
            customCookie = false;
          } else {
            customCookie = cInput || null;
          }
          cachedContext = null;
          console.log('Konfigurasi cookie berhasil diperbarui!');
          await new Promise(r => setTimeout(r, 1200));
          continue;
        }
        default:
          console.log('Pilihan tidak tersedia. Silakan pilih angka 0 sampai 14.');
          await new Promise(r => setTimeout(r, 1200));
          continue;
      }

      if (result !== null) {
        await displayPaginatedJson(rl, result);
      }
    } catch (err) {
      console.error('\n[ERROR]:', err.message || err);
      await rl.question('\nTekan [Enter] untuk kembali ke menu...');
    }
  }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  runCli();
}
