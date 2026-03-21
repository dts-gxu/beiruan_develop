/**
 * AI开发助手 - 需求分析流程模块
 * quickAction, pipelineStep, streamPipelineResult, runFullPipeline, downloadWordReport
 */

// ==================== 快捷操作（需求分析流程） ====================

function quickAction(action) {
	if (isProcessing) return;

	if (action === 'analyze') {
		var doc = getDocContent();
		if (!doc) { layer.msg('请先上传文件或在输入框输入需求文档', {icon: 5}); return; }
		appendMessage('user', '请提取需求文档的关键信息');
		isProcessing = true; $('#btnSend').prop('disabled', true);
		pipelineStep('analyze', doc, '1.3', '关键信息提取', function() { pipelineComplete(); });
	}
	else if (action === 'adapt') {
		var prev = analysisHistory['1.3'];
		if (!prev) { layer.msg('请先执行"提取关键信息"', {icon: 5}); return; }
		appendMessage('user', '请进行RuoYi适配分析');
		isProcessing = true; $('#btnSend').prop('disabled', true);
		pipelineStep('adapt', prev, '1.4', 'RuoYi适配分析', function() { pipelineComplete(); });
	}
	else if (action === 'evaluate') {
		var prev = (analysisHistory['1.3'] || '') + '\n\n' + (analysisHistory['1.4'] || '');
		if (!analysisHistory['1.3']) { layer.msg('请先执行"提取关键信息"', {icon: 5}); return; }
		appendMessage('user', '请进行完整性评估');
		isProcessing = true; $('#btnSend').prop('disabled', true);
		pipelineStep('evaluate', prev, '1.5', '完整性评估', function() { pipelineComplete(); });
	}
	else if (action === 'report') {
		var all = (analysisHistory['1.3'] || '') + '\n\n' + (analysisHistory['1.4'] || '') + '\n\n' + (analysisHistory['1.5'] || '');
		if (!analysisHistory['1.5']) { layer.msg('请先执行"完整性评估"', {icon: 5}); return; }
		appendMessage('user', '请生成需求完整性报告');
		isProcessing = true; $('#btnSend').prop('disabled', true);
		pipelineStep('report', all, '1.6', '完整性报告', function() { pipelineComplete(); });
	}
	else if (action === 'all') {
		var doc = getDocContent();
		if (!doc) { layer.msg('请先上传文件或在输入框输入需求文档', {icon: 5}); return; }
		appendMessage('user', '开始执行完整需求分析流程（文档长度：' + doc.length + '字符）');
		runFullPipeline(doc);
	}
}

function getDocContent() {
	if (analysisHistory['1.1']) return analysisHistory['1.1'];
	var input = $('#userInput').val().trim();
	if (input) {
		analysisHistory['1.1'] = input;
		$('#userInput').val('');
		return input;
	}
	return null;
}

function callStepApi(url, data, stepKey) {
	isProcessing = true;
	$('#btnSend').prop('disabled', true);
	$('#modelInfo').text('DeepSeek V3 | 分析中...');
	showTyping();

	$.ajax({
		url: prefix + url, type: 'POST', data: data, timeout: 180000,
		success: function(res) {
			hideTyping();
			if (res.code == 0) {
				var reply = res.data || res.msg;
				appendMessage('ai', reply);
				analysisHistory[stepKey] = reply;
			} else {
				appendMessage('ai', '错误：' + res.msg);
			}
		},
		error: function(xhr, status, error) {
			hideTyping();
			appendMessage('ai', '请求失败：' + (status === 'timeout' ? '请求超时' : error));
		},
		complete: function() {
			isProcessing = false;
			$('#btnSend').prop('disabled', false);
			$('#modelInfo').text('DeepSeek V3 | 就绪');
		}
	});
}

function runFullPipeline(doc) {
	appendHtmlMessage('ai', '<b>开始全流程分析</b>：提取关键信息 → 适配分析 → 完整性评估 → 生成报告<br><br>预计需要3-8分钟，每步实时流式显示...');
	isProcessing = true;
	$('#btnSend').prop('disabled', true);

	pipelineStep('analyze', doc, '1.3', '关键信息提取', function() {
		pipelineStep('adapt', analysisHistory['1.3'], '1.4', 'RuoYi适配分析', function() {
			var allPrev = analysisHistory['1.3'] + '\n\n' + analysisHistory['1.4'];
			pipelineStep('evaluate', allPrev, '1.5', '完整性评估', function() {
				var reportData = analysisHistory['1.3'] + '\n\n' + analysisHistory['1.4'] + '\n\n' + analysisHistory['1.5'];
				pipelineStep('report', reportData, '1.6', '完整性报告', function() {
					appendHtmlMessage('ai', '<b>全流程分析完成！</b><br><br><button class="btn btn-sm btn-success" onclick="downloadWordReport()" style="margin-top:5px;"><i class="fa fa-download"></i> 下载Word报告</button>');
					pipelineComplete();
				});
			});
		});
	});
}

/* Pipeline流式步骤（两步SSE: prepare POST + stream EventSource） */
function pipelineStep(step, data, stepKey, stepName, onSuccess) {
	$('#modelInfo').text('DeepSeek V3 | ' + stepName + ' 准备中...');

	var prepareData = { step: step, data: data };
	if (selectedKbIds.length > 0) {
		prepareData.kbIds = getKbIdsParam();
	}
	$.ajax({
		url: prefix + '/pipeline/prepare',
		type: 'POST',
		data: prepareData,
		success: function(res) {
			if (res.code == 0 && res.data && res.data.taskId) {
				streamPipelineResult(res.data.taskId, step, data, stepKey, stepName, onSuccess);
			} else {
				appendMessage('ai', stepName + ' 准备失败：' + (res.msg || '未知错误'));
				pipelineComplete();
			}
		},
		error: function() {
			appendMessage('ai', stepName + ' 请求失败');
			pipelineComplete();
		}
	});
}

/* Pipeline SSE流式接收 */
function streamPipelineResult(taskId, step, inputData, stepKey, stepName, onSuccess) {
	$('#welcomePage').remove();
	var safeKey = stepKey.replace(/\./g, '-');
	var bubbleId = 'pipeline-' + safeKey + '-' + Date.now();
	var html = '<div class="chat-message ai">'
		+ '<div class="avatar"><i class="fa fa-robot"></i></div>'
		+ '<div class="bubble" id="' + bubbleId + '"><b>' + stepName + '</b><br><br>'
		+ '<div class="typing-indicator"><span></span><span></span><span></span></div></div></div>';
	$('#chatArea').append(html);
	scrollToBottom();

	var fullText = '';
	var responseQueue = [];
	var closed = false;
	var animFinished = false;
	var bubbleEl = document.getElementById(bubbleId);
	var pipelineQuotes = []; // 知识库引用数据
	var eventSource = new EventSource(prefix + '/pipeline/stream?taskId=' + encodeURIComponent(taskId));
	$('#modelInfo').text('DeepSeek V3 | ' + stepName + ' 生成中...');

	var lastRenderTime = 0;
	if (bubbleEl) bubbleEl.classList.add('streaming');

	function animateResponseText() {
		if (responseQueue.length > 0) {
			var batchSize = Math.max(1, Math.round(responseQueue.length / 30));
			for (var i = 0; i < batchSize && i < responseQueue.length; i++) {
				fullText += responseQueue[i];
			}
			responseQueue = responseQueue.slice(batchSize);
			var now = Date.now();
			if (now - lastRenderTime >= 100) {
				lastRenderTime = now;
				if (bubbleEl) bubbleEl.innerHTML = '<b>' + stepName + '</b><br><br>' + formatMarkdown(trimIncompleteTable(fullText));
				if (!userScrolledUp) scrollToBottom();
			}
		}
		if (animFinished && responseQueue.length === 0) {
			if (bubbleEl) bubbleEl.classList.remove('streaming');
			return;
		}
		requestAnimationFrame(animateResponseText);
	}
	requestAnimationFrame(animateResponseText);

	// 接收知识库引用段落数据
	eventSource.addEventListener('quotes', function(e) {
		try {
			pipelineQuotes = JSON.parse(e.data);
		} catch(ex) {
			console.warn('Failed to parse pipeline quotes:', ex);
		}
	});

	eventSource.onmessage = function(e) {
		responseQueue.push(e.data);
	};

	eventSource.addEventListener('done', function(e) {
		if (closed) return;
		closed = true;
		animFinished = true;
		eventSource.close();
		while (responseQueue.length > 0) { fullText += responseQueue.shift(); }
		var doneText = e.data || fullText;
		analysisHistory[stepKey] = doneText;
		var quotesHtml = '';
		var formattedText = doneText;
		// ====== Pipeline引用标注处理 ======
		console.log('%c[RAG-Pipeline] done事件, quotes数:', 'color:#4e83fd;font-weight:bold', pipelineQuotes.length);
		if (pipelineQuotes.length > 0) {
			lastQuotes = pipelineQuotes;
			quotesHtml = buildQuoteCards(pipelineQuotes);
			// 1. 在纯文本阶段插入[N](CITE)标记
			formattedText = insertCitationMarkersInText(doneText, pipelineQuotes);
			console.log('[RAG-Pipeline] insertCitation后:', formattedText === doneText ? '【未变化】' : '【已插入标记】');
		}
		// 2. 渲染markdown
		var renderedHtml = formatMarkdown(formattedText);
		// 3. 将[N](CITE)替换为蓝色徽章
		if (pipelineQuotes.length > 0) {
			renderedHtml = renderInlineCitations(renderedHtml, pipelineQuotes);
			console.log('[RAG-Pipeline] renderCitations后, rag-cite数:', (renderedHtml.match(/rag-cite/g)||[]).length);
		}
		if (bubbleEl) bubbleEl.innerHTML = '<b>' + stepName + '</b><br><br>' + renderedHtml + quotesHtml;
		scrollToBottom();
		$('#modelInfo').text('DeepSeek V3 | ' + stepName + ' 完成');
		savePipelineMessage(stepName, inputData, doneText);
		if (currentConversationId) {
			messageCache[currentConversationId] = $('#chatArea').html();
		}
		if (onSuccess) onSuccess();
	});

	eventSource.addEventListener('error', function(e) {
		if (closed) return;
		closed = true;
		animFinished = true;
		eventSource.close();
		while (responseQueue.length > 0) { fullText += responseQueue.shift(); }
		if (fullText) {
			analysisHistory[stepKey] = fullText;
			if (bubbleEl) bubbleEl.innerHTML = '<b>' + stepName + '</b><br><br>' + formatMarkdown(fullText);
			savePipelineMessage(stepName, inputData, fullText);
			if (currentConversationId) {
				messageCache[currentConversationId] = $('#chatArea').html();
			}
			if (onSuccess) onSuccess();
		} else {
			if (bubbleEl) bubbleEl.innerHTML = '<span style="color:#e74c3c;">' + stepName + ' 流式连接失败</span>';
			pipelineComplete();
		}
	});

	eventSource.onerror = function(e) {
		if (closed) return;
		closed = true;
		animFinished = true;
		eventSource.close();
		while (responseQueue.length > 0) { fullText += responseQueue.shift(); }
		if (fullText) {
			analysisHistory[stepKey] = fullText;
			if (bubbleEl) bubbleEl.innerHTML = '<b>' + stepName + '</b><br><br>' + formatMarkdown(fullText);
			savePipelineMessage(stepName, inputData, fullText);
			if (currentConversationId) {
				messageCache[currentConversationId] = $('#chatArea').html();
			}
			if (onSuccess) onSuccess();
		} else {
			if (bubbleEl) bubbleEl.innerHTML = '<span style="color:#e74c3c;">' + stepName + ' 连接中断</span>';
			pipelineComplete();
		}
		$('#modelInfo').text('DeepSeek V3 | 就绪');
	};
}

/* 异步保存pipeline消息到会话 */
function savePipelineMessage(stepName, inputData, aiReply) {
	var saveData = {
		userMessage: '[' + stepName + '] ' + (inputData || '').substring(0, 200),
		aiReply: aiReply
	};
	if (currentConversationId) saveData.conversationId = currentConversationId;
	$.ajax({
		url: prefix + '/conversation/saveMessages',
		type: 'POST',
		data: saveData,
		success: function(saveRes) {
			if (saveRes.code == 0 && saveRes.data && saveRes.data.conversationId) {
				if (!currentConversationId) {
					currentConversationId = saveRes.data.conversationId;
					sessionStorage.setItem('ai_currentConvId', currentConversationId);
				}
			}
		}
	});
}

function pipelineComplete() {
	isProcessing = false;
	$('#btnSend').prop('disabled', false);
	$('#modelInfo').text('DeepSeek V3 | 就绪');
	if (currentConversationId) {
		messageCache[currentConversationId] = $('#chatArea').html();
	}
	loadConversations();
}

function callReportApi(analysisData) {
	isProcessing = true;
	$('#btnSend').prop('disabled', true);
	$('#modelInfo').text('DeepSeek V3 | 生成报告...');
	showTyping();

	$.ajax({
		url: prefix + '/report', type: 'POST',
		data: { analysisResult: analysisData }, timeout: 180000,
		success: function(res) {
			hideTyping();
			if (res.code == 0) {
				var report = res.data || res.msg;
				analysisHistory['1.6'] = report;
				appendHtmlMessage('ai', '<b>需求完整性报告</b><br><br>' + formatMarkdown(report)
					+ '<br><br><button class="btn btn-sm btn-success" onclick="downloadWordReport()" style="margin-top:10px;"><i class="fa fa-download"></i> 下载Word报告</button>');
			} else {
				appendMessage('ai', '报告生成失败：' + res.msg);
			}
		},
		error: function(xhr, status, error) {
			hideTyping();
			appendMessage('ai', '报告生成请求失败');
		},
		complete: function() {
			isProcessing = false;
			$('#btnSend').prop('disabled', false);
			$('#modelInfo').text('DeepSeek V3 | 就绪');
		}
	});
}

function downloadWordReport() {
	var report = analysisHistory['1.6'];
	if (!report) { layer.msg('请先生成完整性报告', {icon: 5}); return; }
	var form = document.createElement('form');
	form.method = 'POST';
	form.action = prefix + '/report/download';
	form.style.display = 'none';
	var inp = document.createElement('input');
	inp.type = 'hidden'; inp.name = 'reportContent'; inp.value = report;
	form.appendChild(inp);
	document.body.appendChild(form);
	form.submit();
	document.body.removeChild(form);
}
