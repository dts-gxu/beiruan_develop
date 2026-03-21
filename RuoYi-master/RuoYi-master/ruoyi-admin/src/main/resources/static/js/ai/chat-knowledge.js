/**
 * AI开发助手 - 知识库模块
 * 知识库选择器、RAG引用卡片展示
 */

// ==================== 知识库选择器 ====================

/* 加载可用知识库列表（渲染为可点击的多选标签） */
function loadKnowledgeBases() {
	$.ajax({
		url: prefix + '/knowledge/list',
		type: 'POST',
		success: function(res) {
			if (res.code == 0 && res.data) {
				var container = $('#kbTags');
				container.empty();
				kbCache = {};
				for (var i = 0; i < res.data.length; i++) {
					var kb = res.data[i];
					kbCache[kb.kbId] = kb;
					var isDify = kb.difyDatasetId && kb.difyDatasetId.length > 0;
					var docInfo = kb.documentCount ? ' (' + kb.documentCount + '篇)' : '';
					var difyIcon = isDify ? '<i class="fa fa-cloud" style="font-size:9px;"></i> ' : '';
					var isActive = selectedKbIds.indexOf(String(kb.kbId)) >= 0;
					var cls = 'kb-tag' + (isActive ? ' active' : '');
					container.append(
						'<span class="' + cls + '" data-kb-id="' + kb.kbId + '" onclick="toggleKb(this)">'
						+ '<i class="fa fa-check"></i> '
						+ difyIcon + escapeHtml(kb.kbName)
						+ '<span class="kb-tag-count">' + docInfo + '</span>'
						+ '</span>'
					);
				}
				if (res.data.length === 0) {
					container.append('<span style="color:#999; font-size:11px;">暂无知识库，请先在知识库管理中创建</span>');
				}
				updateKbInfoPanel();
			}
		}
	});
}

/* 切换知识库选中状态（多选） */
function toggleKb(el) {
	var $el = $(el);
	var kbId = String($el.data('kb-id'));
	var idx = selectedKbIds.indexOf(kbId);
	if (idx >= 0) {
		selectedKbIds.splice(idx, 1);
		$el.removeClass('active');
	} else {
		selectedKbIds.push(kbId);
		$el.addClass('active');
	}
	if (selectedKbIds.length > 0) {
		sessionStorage.setItem('ai_selectedKbIds', JSON.stringify(selectedKbIds));
	} else {
		sessionStorage.removeItem('ai_selectedKbIds');
	}
	updateKbInfoPanel();
}

/* 更新知识库信息面板 */
function updateKbInfoPanel() {
	if (selectedKbIds.length > 0) {
		var names = [];
		var totalDocs = 0;
		for (var i = 0; i < selectedKbIds.length; i++) {
			var kb = kbCache[selectedKbIds[i]];
			if (kb) {
				names.push(kb.kbName);
				totalDocs += (kb.documentCount || 0);
			}
		}
		$('#kbInfoName').text('已选 ' + selectedKbIds.length + ' 个知识库 (' + totalDocs + '篇文档)');
		$('#kbInfoDesc').text(names.join('、'));
		$('#kbInfoPanel').show();
	} else {
		$('#kbInfoPanel').hide();
	}
}

/* 获取逗号分隔的kbIds字符串 */
function getKbIdsParam() {
	return selectedKbIds.join(',');
}

/* 打开知识库管理页面 */
function openKbManage() {
	$.modal.openTab("知识库管理", ctx + "ai/knowledge");
}

// ==================== RAG引用展示（仿FastGPT：嵌在AI回复内，默认展开的文档引用卡片） ====================

/**
 * 根据文件名推断图标CSS类和字符
 */
function getDocIcon(docName) {
	if (!docName) return { cls: 'default', ch: 'D' };
	var n = docName.toLowerCase();
	if (n.endsWith('.pdf')) return { cls: 'pdf', ch: 'P' };
	if (n.endsWith('.doc') || n.endsWith('.docx')) return { cls: 'word', ch: 'W' };
	if (n.endsWith('.xls') || n.endsWith('.xlsx')) return { cls: 'excel', ch: 'X' };
	if (n.endsWith('.md')) return { cls: 'default', ch: 'M' };
	if (n.endsWith('.txt')) return { cls: 'default', ch: 'T' };
	return { cls: 'default', ch: 'D' };
}

/**
 * 构建引用文档卡片HTML（仿FastGPT，嵌在AI气泡内，默认展开）
 * 每个被引用的文档生成一张卡片，显示文档图标+文档名+操作链接+引用内容
 * @param {Array} quotes - [{docName, content, score, kbName, kbId}]
 * @returns {string} HTML
 */
function buildQuoteCards(quotes) {
	if (!quotes || quotes.length === 0) return '';

	// 按文档名分组（同一文档多段引用合并为一张卡片）
	var groups = [];
	var groupMap = {};
	for (var i = 0; i < quotes.length; i++) {
		var name = quotes[i].docName || '未命名文档';
		if (groupMap[name] === undefined) {
			groupMap[name] = groups.length;
			groups.push({ docName: name, kbName: quotes[i].kbName, score: quotes[i].score, segments: [], docId: quotes[i].docId, docType: quotes[i].docType });
		}
		groups[groupMap[name]].segments.push(quotes[i]);
		if (quotes[i].score > groups[groupMap[name]].score) {
			groups[groupMap[name]].score = quotes[i].score;
		}
		if (!groups[groupMap[name]].docId && quotes[i].docId) {
			groups[groupMap[name]].docId = quotes[i].docId;
		}
	}

	var html = '';
	for (var g = 0; g < groups.length; g++) {
		var grp = groups[g];
		var icon = getDocIcon(grp.docName);
		var scorePercent = grp.score ? Math.round(grp.score * 100) : 0;
		var cardId = 'rag-card-' + Date.now() + '-' + g;

		html += '<div class="rag-quote-card" id="' + cardId + '">';
		html += '<div class="rag-quote-card-header" onclick="toggleQuoteCard(\'' + cardId + '\')">';
		html += '<div class="doc-icon ' + icon.cls + '">' + icon.ch + '</div>';
		html += '<span class="doc-title">' + escapeHtml(grp.docName) + '</span>';
		html += '<div class="doc-actions">';
		html += '<a onclick="event.stopPropagation(); showQuoteFullPopup(' + groupMap[grp.docName] + ')" title="查看引用详情"><i class="fa fa-eye"></i> 全部引用</a>';
		if (grp.docId) {
			html += '<a onclick="event.stopPropagation(); viewDocumentFullContent(' + grp.docId + ')" title="查看文档全文"><i class="fa fa-file-text-o"></i> 全文</a>';
			html += '<a onclick="event.stopPropagation(); downloadDocument(' + grp.docId + ')" title="下载原始文件"><i class="fa fa-download"></i> 下载</a>';
		}
		html += '</div>';
		html += '<i class="fa fa-chevron-down doc-arrow"></i>';
		html += '</div>';

		html += '<div class="rag-quote-card-body">';
		for (var s = 0; s < grp.segments.length; s++) {
			if (s > 0) html += '<hr style="border:none;border-top:1px dashed #e0e0e0;margin:10px 0;">';
			var segContent = grp.segments[s].content || '（无内容）';
			html += escapeHtml(segContent.length > 300 ? segContent.substring(0, 300) + '...' : segContent);
		}
		html += '</div>';

		html += '<div class="rag-quote-card-meta">';
		if (grp.kbName) html += '<span><i class="fa fa-database"></i>' + escapeHtml(grp.kbName) + '</span>';
		html += '<span><i class="fa fa-bar-chart"></i>相关度 ' + scorePercent + '%</span>';
		if (grp.segments.length > 1) html += '<span>共 ' + grp.segments.length + ' 段引用</span>';
		html += '</div>';
		html += '</div>';
	}
	return html;
}

/**
 * 点击卡片头部，展开/收起引用内容
 */
function toggleQuoteCard(cardId) {
	var $card = $('#' + cardId);
	var $body = $card.find('.rag-quote-card-body');
	var $meta = $card.find('.rag-quote-card-meta');
	var $arrow = $card.find('.doc-arrow');
	if ($body.is(':visible')) {
		$body.slideUp(200);
		$meta.slideUp(200);
		$arrow.removeClass('expanded');
	} else {
		$body.slideDown(200);
		$meta.slideDown(200).css('display', 'flex');
		$arrow.addClass('expanded');
	}
}

/**
 * 【核心】在AI回复纯文本中自动匹配知识库内容并插入[N]标记
 * 不依赖AI模型生成标记，程序通过子串匹配自动判定引用关系
 * 调用时机：在formatMarkdown之前，对纯文本操作
 * @param {string} text - AI回复的纯文本（未经markdown渲染）
 * @param {Array} quotes - 引用段落数组
 * @returns {string} 插入[N]标记后的纯文本
 */
function insertCitationMarkersInText(text, quotes) {
	if (!text || !quotes || quotes.length === 0) return text;
	// 只有AI已生成[N](CITE)时跳过（不检测裸[N]，避免误判）
	if (/\[\d{1,2}\]\(CITE\)/.test(text)) return text;

	console.log('[RAG-CITE] 开始标注引用, 文本长度:', text.length, '引用数:', quotes.length);

	// === 4字符滑动窗口n-gram匹配（无需中文分词，适合AI改写场景） ===
	var N = 4; // n-gram大小

	// 从文本中提取n-gram集合（去除标点和空白后的连续字符滑动窗口）
	function extractNgrams(str) {
		if (!str) return [];
		var clean = str.replace(/[\s\r\n\t.,，。！？；：、（）()\[\]【】""''《》<>\/\\—\-+=#@!~`^&%$|{}\*:：]/g, '');
		var grams = [];
		for (var i = 0; i <= clean.length - N; i++) {
			grams.push(clean.substring(i, i + N));
		}
		return grams;
	}

	// 预计算每条引用的n-gram Set
	var quoteNgramSets = [];
	for (var qi = 0; qi < quotes.length; qi++) {
		var grams = extractNgrams(quotes[qi].content || '');
		var set = {};
		for (var g = 0; g < grams.length; g++) set[grams[g]] = true;
		quoteNgramSets.push(set);
	}

	// 按中文句号/问号/叹号/换行拆分为句子（保留分隔符）
	var parts = text.split(/([\u3002\uff01\uff1f\uff1b\n])/); // 。！？；\n
	// 重组为 {text, delim} 对
	var sentPairs = [];
	for (var i = 0; i < parts.length; i += 2) {
		sentPairs.push({
			text: parts[i] || '',
			delim: (i + 1 < parts.length) ? parts[i + 1] : ''
		});
	}

	console.log('[RAG-CITE] 拆分句子数:', sentPairs.length);

	var output = '';
	var totalCites = 0;

	for (var si = 0; si < sentPairs.length; si++) {
		var sent = sentPairs[si].text;
		var delim = sentPairs[si].delim;
		var clean = sent.replace(/\s+/g, '');

		// 跳过太短的句子
		if (clean.length < 8) {
			output += sent + delim;
			continue;
		}

		// 提取句子的n-gram集合
		var sentGrams = extractNgrams(clean);
		if (sentGrams.length < 2) {
			output += sent + delim;
			continue;
		}

		// 与每条引用计算n-gram命中数
		var matched = [];
		var debugScores = [];
		for (var qi = 0; qi < quoteNgramSets.length; qi++) {
			var qset = quoteNgramSets[qi];
			var hits = 0;
			for (var gi = 0; gi < sentGrams.length; gi++) {
				if (qset[sentGrams[gi]]) hits++;
			}
			var coverage = sentGrams.length > 0 ? hits / sentGrams.length : 0;
			debugScores.push('Q' + (quotes[qi].index||(qi+1)) + ':hits=' + hits + '/coverage=' + (coverage*100).toFixed(1) + '%');
			if (hits >= 3 && coverage >= 0.15) {
				matched.push({ idx: quotes[qi].index || (qi + 1), score: coverage, hits: hits });
			}
		}
		console.log('[RAG-CITE] 句' + (si+1) + '(ngrams=' + sentGrams.length + '): "' + clean.substring(0,30) + '..."', debugScores.join(' | '));

		// 取得分最高的 top 2
		matched.sort(function(a, b) { return b.score - a.score; });
		var markers = '';
		var used = {};
		for (var mi = 0; mi < Math.min(2, matched.length); mi++) {
			var qidx = matched[mi].idx;
			if (!used[qidx]) {
				markers += '[' + qidx + '](CITE)';
				used[qidx] = true;
				totalCites++;
			}
		}

		output += sent + markers + delim;
	}

	console.log('[RAG-CITE] 标注完成, 共标注', totalCites, '处引用');
	return output;
}

/**
 * 将AI回复HTML中的引用标记替换为可交互的内联引用徽章（蓝色圆形）
 * 支持两种格式：
 *   1. [N](CITE) — AI生成的markdown链接格式（首选，最可靠）
 *   2. [N] — 纯数字方括号格式（兜底）
 * 悬浮显示对应引用段落的tooltip
 * @param {string} html - AI回复的HTML内容（已经过formatMarkdown）
 * @param {Array} quotes - 引用段落数组，每项含 index/docName/content/kbName
 * @returns {string} 替换后的HTML
 */
function renderInlineCitations(html, quotes) {
	console.log('[RAG-RENDER] renderInlineCitations被调用, html长度:', (html||'').length, 'quotes数:', (quotes||[]).length);
	if (!html || !quotes || quotes.length === 0) return html;

	// 构建 index → quote 的映射
	var quoteMap = {};
	for (var i = 0; i < quotes.length; i++) {
		var idx = quotes[i].index || (i + 1);
		quoteMap[idx] = quotes[i];
	}
	console.log('[RAG-RENDER] quoteMap keys:', Object.keys(quoteMap).join(','));

	// 辅助函数：生成徽章HTML（点击固定显示tooltip）
	function buildBadge(n) {
		var q = quoteMap[n];
		if (!q) { console.log('[RAG-RENDER] buildBadge: quoteMap中找不到index=' + n); return null; }
		var docName = escapeHtml(q.docName || '未命名');
		var kbName = escapeHtml(q.kbName || '');
		var snippet = escapeHtml((q.content || '').substring(0, 200));
		if ((q.content || '').length > 200) snippet += '...';
		var actionBtns = '<span class="cite-tooltip-actions">';
		if (q.docId) {
			actionBtns += '<a onclick="event.stopPropagation();viewDocumentFullContent(' + q.docId + ')" title="查看文档全文"><i class="fa fa-file-text-o"></i> 查看全文</a>';
			actionBtns += '<a onclick="event.stopPropagation();downloadDocument(' + q.docId + ')" title="下载原始文件"><i class="fa fa-download"></i> 下载</a>';
		} else {
			// 无docId时，用quote自身内容展示
			actionBtns += '<a onclick="event.stopPropagation();viewQuoteContent(' + n + ')" title="查看引用内容"><i class="fa fa-eye"></i> 查看引用内容</a>';
		}
		actionBtns += '</span>';
		return '<span class="rag-cite" onclick="toggleCiteTooltip(event, this)">' + n
			+ '<span class="rag-cite-tooltip">'
			+ '<span class="cite-close" onclick="event.stopPropagation();closeCiteTooltip(this)">&times;</span>'
			+ '<span class="cite-doc-name"><i class="fa fa-file-text-o"></i> ' + docName + '</span>'
			+ (kbName ? '<span class="cite-kb-name"><i class="fa fa-database"></i> ' + kbName + '</span>' : '')
			+ '<span class="cite-snippet">' + snippet + '</span>'
			+ actionBtns
			+ '</span></span>';
	}

	// 第一优先：匹配 [N](CITE) 格式
	var hasCiteLinks = /\[\d{1,2}\]\(CITE\)/.test(html);
	console.log('[RAG-RENDER] 检测[N](CITE):', hasCiteLinks);
	if (hasCiteLinks) {
		var result = html.replace(/\[(\d{1,2})\]\(CITE\)/g, function(match, num) {
			console.log('[RAG-RENDER] 替换:', match, '-> 徽章' + num);
			var badge = buildBadge(parseInt(num, 10));
			return badge || '';
		});
		return result;
	}

	// 第二优先：匹配 <a href="CITE">N</a> 格式
	var hasCiteAnchors = /<a[^>]*href="CITE"[^>]*>/.test(html);
	console.log('[RAG-RENDER] 检测<a href=CITE>:', hasCiteAnchors);
	if (hasCiteAnchors) {
		return html.replace(/<a[^>]*href="CITE"[^>]*>(\d{1,2})<\/a>/g, function(match, num) {
			var badge = buildBadge(parseInt(num, 10));
			return badge || '';
		});
	}

	// 第三兜底：匹配 [N] 格式（排除markdown链接 [text](url)）
	console.log('[RAG-RENDER] 使用兜底[N]匹配');
	var bareMatches = html.match(/\[\d{1,2}\](?!\()/g);
	console.log('[RAG-RENDER] 找到裸[N]:', bareMatches);
	return html.replace(/\[(\d{1,2})\](?!\()/g, function(match, num) {
		var badge = buildBadge(parseInt(num, 10));
		return badge || match;
	});
}

/**
 * 兼容旧调用
 */
function buildInlineCiteMarkers() { return ''; }

/**
 * 弹窗查看完整引用内容（"全部引用"按钮触发）
 */
function showQuoteFullPopup(groupIndex) {
	if (!lastQuotes || lastQuotes.length === 0) return;

	// 重建分组
	var groups = [];
	var groupMap = {};
	for (var i = 0; i < lastQuotes.length; i++) {
		var name = lastQuotes[i].docName || '未命名文档';
		if (groupMap[name] === undefined) {
			groupMap[name] = groups.length;
			groups.push({ docName: name, kbName: lastQuotes[i].kbName, score: lastQuotes[i].score, segments: [], docId: lastQuotes[i].docId });
		}
		groups[groupMap[name]].segments.push(lastQuotes[i]);
		if (!groups[groupMap[name]].docId && lastQuotes[i].docId) {
			groups[groupMap[name]].docId = lastQuotes[i].docId;
		}
	}

	if (groupIndex >= groups.length) return;
	var grp = groups[groupIndex];
	var icon = getDocIcon(grp.docName);
	var scorePercent = grp.score ? Math.round(grp.score * 100) : 0;

	var content = '<div style="padding:0;">';
	content += '<div style="padding:12px 16px; background:#f7f8fa; border-bottom:1px solid #e4e7ed; display:flex; align-items:center; gap:10px;">';
	content += '<div style="width:32px;height:32px;border-radius:6px;background:' + (icon.cls==='word'?'#2b579a':icon.cls==='pdf'?'#e74c3c':icon.cls==='excel'?'#27ae60':'#4e83fd') + ';color:#fff;display:flex;align-items:center;justify-content:center;font-size:16px;font-weight:700;">' + icon.ch + '</div>';
	content += '<div style="flex:1;"><div style="font-weight:600;font-size:14px;color:#333;">' + escapeHtml(grp.docName) + '</div>';
	content += '<div style="font-size:11px;color:#888;margin-top:2px;">';
	if (grp.kbName) content += '<i class="fa fa-database"></i> ' + escapeHtml(grp.kbName) + ' · ';
	content += '相关度 ' + scorePercent + '%';
	if (grp.segments.length > 1) content += ' · 共 ' + grp.segments.length + ' 段引用';
	content += '</div></div>';
	// 操作按钮
	if (grp.docId) {
		content += '<div style="display:flex;gap:8px;">';
		content += '<a onclick="viewDocumentFullContent(' + grp.docId + ')" style="cursor:pointer;color:#4e83fd;font-size:12px;white-space:nowrap;" title="查看文档全文"><i class="fa fa-file-text-o"></i> 查看全文</a>';
		content += '<a onclick="downloadDocument(' + grp.docId + ')" style="cursor:pointer;color:#4e83fd;font-size:12px;white-space:nowrap;" title="下载原始文件"><i class="fa fa-download"></i> 下载</a>';
		content += '</div>';
	}
	content += '</div>';

	content += '<div style="padding:14px 16px; max-height:420px; overflow-y:auto;">';
	for (var s = 0; s < grp.segments.length; s++) {
		if (grp.segments.length > 1) {
			content += '<div style="font-size:11px; color:#4e83fd; font-weight:600; margin-bottom:4px;">引用段落 ' + (s+1) + '</div>';
		}
		content += '<div style="background:#f9f9fb; border:1px solid #eee; border-radius:6px; padding:12px; font-size:13px; line-height:1.8; white-space:pre-wrap; word-break:break-all; margin-bottom:10px;">';
		content += escapeHtml(grp.segments[s].content || '（无内容）');
		content += '</div>';
	}
	content += '</div></div>';

	layer.open({
		type: 1,
		title: false,
		area: ['680px', '520px'],
		content: content,
		shadeClose: true
	});
}

/**
 * 查看知识库文档全文（弹窗展示）
 */
function viewDocumentFullContent(docId) {
	$.ajax({
		url: prefix + '/knowledge/document/content',
		type: 'GET',
		data: { docId: docId },
		success: function(res) {
			if (res.code == 0 && res.data) {
				var doc = res.data;
				var icon = getDocIcon(doc.docName);
				var iconColor = icon.cls==='word'?'#2b579a':icon.cls==='pdf'?'#e74c3c':icon.cls==='excel'?'#27ae60':'#4e83fd';
				var html = '<div style="padding:0;">';
				// 标题栏
				html += '<div style="padding:14px 18px; background:#f7f8fa; border-bottom:1px solid #e4e7ed; display:flex; align-items:center; gap:10px;">';
				html += '<div style="width:36px;height:36px;border-radius:6px;background:' + iconColor + ';color:#fff;display:flex;align-items:center;justify-content:center;font-size:18px;font-weight:700;">' + icon.ch + '</div>';
				html += '<div style="flex:1;"><div style="font-weight:600;font-size:15px;color:#333;">' + escapeHtml(doc.docName || '未命名') + '</div>';
				html += '<div style="font-size:11px;color:#888;margin-top:3px;">';
				if (doc.docType) html += '类型：' + escapeHtml(doc.docType) + ' · ';
				if (doc.wordCount) html += doc.wordCount + ' 字';
				html += '</div></div>';
				// 下载按钮
				html += '<a onclick="downloadDocument(' + docId + ')" style="cursor:pointer;background:#4e83fd;color:#fff;padding:6px 14px;border-radius:4px;font-size:12px;text-decoration:none;white-space:nowrap;"><i class="fa fa-download"></i> 下载文件</a>';
				html += '</div>';
				// 全文内容
				html += '<div style="padding:16px 18px; max-height:500px; overflow-y:auto;">';
				if (doc.content) {
					html += '<div style="background:#f9f9fb; border:1px solid #eee; border-radius:8px; padding:16px; font-size:13px; line-height:1.9; white-space:pre-wrap; word-break:break-all;">';
					html += escapeHtml(doc.content);
					html += '</div>';
				} else {
					html += '<div style="text-align:center;color:#999;padding:40px;"><i class="fa fa-file-o" style="font-size:32px;"></i><br><br>该文档无文本内容（可能是纯文件类型）<br>请点击右上角下载查看原始文件</div>';
				}
				html += '</div></div>';

				layer.open({
					type: 1,
					title: false,
					area: ['750px', '600px'],
					content: html,
					shadeClose: true
				});
			} else {
				layer.msg(res.msg || '获取文档内容失败', {icon: 5});
			}
		},
		error: function() {
			layer.msg('请求失败，请稍后重试', {icon: 5});
		}
	});
}

/**
 * 下载知识库文档原始文件
 */
function downloadDocument(docId) {
	window.open(prefix + '/knowledge/document/download?docId=' + docId, '_blank');
}

/**
 * 点击引用徽章：切换tooltip固定显示/隐藏
 * 同一时间只显示一个tooltip
 */
function toggleCiteTooltip(event, el) {
	event.stopPropagation();
	var $el = $(el);
	var isActive = $el.hasClass('active');
	// 先关闭所有已打开的tooltip
	$('.rag-cite.active').removeClass('active');
	if (!isActive) {
		$el.addClass('active');
	}
}

/**
 * 关闭tooltip（点击×按钮）
 */
function closeCiteTooltip(closeBtn) {
	$(closeBtn).closest('.rag-cite').removeClass('active');
}

/**
 * 查看引用内容（无docId时的fallback，用lastQuotes数据在弹窗中展示）
 */
function viewQuoteContent(quoteIndex) {
	if (!lastQuotes || lastQuotes.length === 0) return;
	var q = null;
	for (var i = 0; i < lastQuotes.length; i++) {
		var idx = lastQuotes[i].index || (i + 1);
		if (idx == quoteIndex) { q = lastQuotes[i]; break; }
	}
	if (!q) return;
	var icon = getDocIcon(q.docName);
	var iconColor = icon.cls==='word'?'#2b579a':icon.cls==='pdf'?'#e74c3c':icon.cls==='excel'?'#27ae60':'#4e83fd';
	var html = '<div style="padding:0;">';
	html += '<div style="padding:14px 18px; background:#f7f8fa; border-bottom:1px solid #e4e7ed; display:flex; align-items:center; gap:10px;">';
	html += '<div style="width:36px;height:36px;border-radius:6px;background:' + iconColor + ';color:#fff;display:flex;align-items:center;justify-content:center;font-size:18px;font-weight:700;">' + icon.ch + '</div>';
	html += '<div style="flex:1;"><div style="font-weight:600;font-size:15px;color:#333;">' + escapeHtml(q.docName || '未命名') + '</div>';
	html += '<div style="font-size:11px;color:#888;margin-top:3px;">';
	if (q.kbName) html += '<i class="fa fa-database"></i> ' + escapeHtml(q.kbName);
	html += '</div></div></div>';
	html += '<div style="padding:16px 18px; max-height:500px; overflow-y:auto;">';
	html += '<div style="background:#f9f9fb; border:1px solid #eee; border-radius:8px; padding:16px; font-size:13px; line-height:1.9; white-space:pre-wrap; word-break:break-all;">';
	html += escapeHtml(q.content || '（无内容）');
	html += '</div></div></div>';
	layer.open({ type: 1, title: false, area: ['700px', '500px'], content: html, shadeClose: true });
}

// 全局点击事件：点击页面其他区域关闭所有tooltip
$(document).on('click', function(e) {
	if (!$(e.target).closest('.rag-cite').length) {
		$('.rag-cite.active').removeClass('active');
	}
});
