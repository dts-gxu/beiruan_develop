/**
 * AI开发助手 - AI聊天模块
 * callAiChat (POST), callAiChatStream (SSE)
 */

// ==================== AI聊天（带会话持久化） ====================

/* 调用AI聊天（POST模式，后端自动创建会话） */
function callAiChat(message) {
	var isNewChat = !currentConversationId;
	isProcessing = true;
	$('#btnSend').prop('disabled', true);
	var kbInfo = selectedKbIds.length > 0 ? ' | RAG(' + selectedKbIds.length + ')' : '';
	$('#modelInfo').text('DeepSeek V3' + kbInfo + ' | 思考中...');
	showTyping();

	var postData = { content: message };
	if (currentConversationId) {
		postData.conversationId = currentConversationId;
	}
	var chatUrl = prefix + '/chat/conversation';
	if (selectedKbIds.length > 0) {
		chatUrl = prefix + '/chat/knowledge';
		postData.kbIds = getKbIdsParam();
	}

	$.ajax({
		url: chatUrl,
		type: 'POST',
		data: postData,
		timeout: 180000,
		success: function(res) {
			hideTyping();
			if (res.code == 0 && res.data) {
				var data = res.data;
				var answer = data.answer || '';

				if (data.conversationId) {
					currentConversationId = data.conversationId;
					sessionStorage.setItem('ai_currentConvId', currentConversationId);
				}

				// 处理引用段落数据
				var quotesHtml = '';
				if (data.quotes && data.quotes.length > 0) {
					lastQuotes = data.quotes;
					quotesHtml = buildQuoteCards(data.quotes);
				}

				// 追加AI回复+底部文档引用标签
				$('#welcomePage').remove();
				var icon = '<i class="fa fa-robot"></i>';
				var citedAnswer = (data.quotes && data.quotes.length > 0) ? insertCitationMarkersInText(answer, data.quotes) : answer;
				var formattedAnswer = formatMarkdown(citedAnswer);
				if (data.quotes && data.quotes.length > 0) {
					formattedAnswer = renderInlineCitations(formattedAnswer, data.quotes);
				}
				var html = '<div class="chat-message ai">'
					+ '<div class="avatar">' + icon + '</div>'
					+ '<div class="bubble">' + formattedAnswer + quotesHtml + '</div>'
					+ '</div>';
				$('#chatArea').append(html);
				scrollToBottom();

				analysisHistory[currentConversationId + '_last'] = answer;

				var info = '';
				if (data.model) info += data.model;
				if (data.costTime) info += ' | ' + (data.costTime / 1000).toFixed(1) + 's';
				$('#modelInfo').text(info + ' | 就绪');

				if (currentConversationId) {
					messageCache[currentConversationId] = $('#chatArea').html();
				}
				if (isNewChat && currentConversationId) {
					var autoTitle = message.substring(0, 30) + (message.length > 30 ? '...' : '');
					$('#currentTitle').html('<i class="fa fa-comments"></i> ' + escapeHtml(autoTitle));
					onUpdateHistoryTitle(currentConversationId, autoTitle);
				} else if (currentConversationId) {
					onUpdateHistoryTitle(currentConversationId, message.substring(0, 30));
				}
			} else {
				appendMessage('ai', '错误：' + (res.msg || '未知错误'));
			}
		},
		error: function(xhr, status, error) {
			hideTyping();
			if (status === 'timeout') {
				appendMessage('ai', '请求超时，请稍后重试。建议缩短输入内容长度。');
			} else {
				appendMessage('ai', '网络错误：' + error);
			}
		},
		complete: function() {
			isProcessing = false;
			$('#btnSend').prop('disabled', false);
			$('#modelInfo').text('DeepSeek V3 | 就绪');
		}
	});
}

/* SSE流式AI聊天（仿FastGPT streamFetch + animateResponseText） */
function callAiChatStream(message) {
	var isNewChat = !currentConversationId;
	isProcessing = true;
	$('#btnSend').prop('disabled', true);
	var kbInfo = selectedKbIds.length > 0 ? ' | RAG(' + selectedKbIds.length + ')' : '';
	$('#modelInfo').text('DeepSeek V3' + kbInfo + ' | 思考中...');

	$('#welcomePage').remove();
	var bubbleId = 'stream-bubble-' + Date.now();
	var icon = '<i class="fa fa-robot"></i>';
	var html = '<div class="chat-message ai">'
		+ '<div class="avatar">' + icon + '</div>'
		+ '<div class="bubble" id="' + bubbleId + '"><div class="typing-indicator"><span></span><span></span><span></span></div></div>'
		+ '</div>';
	$('#chatArea').append(html);
	scrollToBottom();

	var streamPath = selectedKbIds.length > 0 ? '/chat/stream/knowledge' : '/chat/stream/conversation';
	var url = prefix + streamPath + '?message=' + encodeURIComponent(message);
	if (currentConversationId) {
		url += '&conversationId=' + currentConversationId;
	}
	if (selectedKbIds.length > 0) {
		url += '&kbIds=' + encodeURIComponent(getKbIdsParam());
	}

	var fullText = '';
	var responseQueue = [];
	var finished = false;
	var bubbleEl = document.getElementById(bubbleId);
	var streamQuotes = []; // SSE中接收到的引用数据

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
				if (bubbleEl) bubbleEl.innerHTML = formatMarkdown(trimIncompleteTable(fullText));
				if (!userScrolledUp) scrollToBottom();
			}
		}
		if (finished && responseQueue.length === 0) {
			if (bubbleEl) {
				bubbleEl.classList.remove('streaming');
				var quotesHtml = '';
				// ====== RAG引用调试 START ======
				console.log('%c[RAG调试] SSE完成，开始处理引用', 'color:#4e83fd;font-weight:bold');
				console.log('[RAG调试] fullText长度:', fullText.length, '前100字:', fullText.substring(0, 100));
				console.log('[RAG调试] streamQuotes数量:', streamQuotes.length);
				if (streamQuotes.length > 0) {
					console.log('[RAG调试] 第1条quote:', JSON.stringify(streamQuotes[0]).substring(0, 200));
					console.log('[RAG调试] quote字段检查:', 'index=' + streamQuotes[0].index, 'content长度=' + (streamQuotes[0].content||'').length, 'docName=' + streamQuotes[0].docName);
				}
				var citedText = (streamQuotes.length > 0) ? insertCitationMarkersInText(fullText, streamQuotes) : fullText;
				console.log('[RAG调试] insertCitationMarkersInText后:', citedText === fullText ? '【未变化】' : '【已插入标记】');
				if (citedText !== fullText) {
					// 找出插入的标记
					var citeMatches = citedText.match(/\[\d{1,2}\]\(CITE\)/g);
					console.log('[RAG调试] 插入的CITE标记:', citeMatches);
				}
				var formattedFinal = formatMarkdown(citedText);
				console.log('[RAG调试] formatMarkdown后, [N](CITE)是否存在:', /\[\d{1,2}\]\(CITE\)/.test(formattedFinal));
				if (streamQuotes.length > 0) {
					lastQuotes = streamQuotes;
					quotesHtml = buildQuoteCards(streamQuotes);
					var beforeRender = formattedFinal;
					formattedFinal = renderInlineCitations(formattedFinal, streamQuotes);
					console.log('[RAG调试] renderInlineCitations后:', beforeRender === formattedFinal ? '【未变化】' : '【已替换为徽章】');
					console.log('[RAG调试] 最终HTML中rag-cite数量:', (formattedFinal.match(/rag-cite/g)||[]).length);
				}
				console.log('%c[RAG调试] 处理完成', 'color:#4e83fd;font-weight:bold');
				// ====== RAG引用调试 END ======
				bubbleEl.innerHTML = formattedFinal + quotesHtml;
			}
			scrollToBottom();
			return;
		}
		requestAnimationFrame(animateResponseText);
	}
	requestAnimationFrame(animateResponseText);

	var eventSource = new EventSource(url);

	eventSource.addEventListener('conversationId', function(e) {
		var newConvId = parseInt(e.data);
		if (newConvId) {
			currentConversationId = newConvId;
			sessionStorage.setItem('ai_currentConvId', currentConversationId);
			if (isNewChat) {
				var autoTitle = message.substring(0, 30) + (message.length > 30 ? '...' : '');
				$('#currentTitle').html('<i class="fa fa-comments"></i> ' + escapeHtml(autoTitle));
			}
		}
	});

	// 接收引用段落数据
	eventSource.addEventListener('quotes', function(e) {
		try {
			streamQuotes = JSON.parse(e.data);
		} catch(ex) {
			console.warn('Failed to parse quotes:', ex);
		}
	});

	eventSource.onmessage = function(e) {
		responseQueue.push(e.data);
		$('#modelInfo').text('DeepSeek V3 | 生成中...');
	};

	eventSource.addEventListener('done', function(e) {
		eventSource.close();
		finished = true;
		while (responseQueue.length > 0) {
			fullText += responseQueue.shift();
		}
		analysisHistory[currentConversationId + '_last'] = fullText;
		if (currentConversationId) {
			messageCache[currentConversationId] = $('#chatArea').html();
		}
		$('#modelInfo').text('DeepSeek V3 | 就绪');
		isProcessing = false;
		$('#btnSend').prop('disabled', false);
		if (isNewChat && currentConversationId) {
			var autoTitle = message.substring(0, 30) + (message.length > 30 ? '...' : '');
			onUpdateHistoryTitle(currentConversationId, autoTitle);
		} else if (currentConversationId) {
			onUpdateHistoryTitle(currentConversationId, message.substring(0, 30));
		}
	});

	eventSource.onerror = function(e) {
		eventSource.close();
		finished = true;
		while (responseQueue.length > 0) {
			fullText += responseQueue.shift();
		}
		if (!fullText) {
			if (bubbleEl) bubbleEl.innerHTML = '<span style="color:#e74c3c;">连接中断，请刷新对话查看已保存的内容。</span>';
		} else {
			if (bubbleEl) bubbleEl.innerHTML = formatMarkdown(fullText);
			if (currentConversationId) {
				messageCache[currentConversationId] = $('#chatArea').html();
			}
		}
		$('#modelInfo').text('DeepSeek V3 | 就绪');
		isProcessing = false;
		$('#btnSend').prop('disabled', false);
		if (isNewChat && currentConversationId) {
			onUpdateHistoryTitle(currentConversationId, message.substring(0, 30));
		}
	};
}
