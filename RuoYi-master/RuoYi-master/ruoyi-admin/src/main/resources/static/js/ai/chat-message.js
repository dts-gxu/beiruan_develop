/**
 * AI开发助手 - 消息显示模块
 * appendMessage, appendHtmlMessage, showTyping, hideTyping, sendMessage, handleKeyDown
 */

function appendMessage(role, content) {
	$('#welcomePage').remove();
	var icon = role === 'ai' ? '<i class="fa fa-robot"></i>' : '<i class="fa fa-user"></i>';
	var html = '<div class="chat-message ' + role + '">'
		+ '<div class="avatar">' + icon + '</div>'
		+ '<div class="bubble">' + formatMarkdown(content) + '</div>'
		+ '</div>';
	$('#chatArea').append(html);
	scrollToBottom();
}

/* 追加包含HTML的系统消息（不做转义） */
function appendHtmlMessage(role, htmlContent) {
	$('#welcomePage').remove();
	var icon = role === 'ai' ? '<i class="fa fa-robot"></i>' : '<i class="fa fa-user"></i>';
	var html = '<div class="chat-message ' + role + '">'
		+ '<div class="avatar">' + icon + '</div>'
		+ '<div class="bubble">' + htmlContent + '</div>'
		+ '</div>';
	$('#chatArea').append(html);
	scrollToBottom();
}

function showTyping() {
	var html = '<div class="chat-message ai" id="typingIndicator">'
		+ '<div class="avatar"><i class="fa fa-robot"></i></div>'
		+ '<div class="bubble"><div class="typing-indicator"><span></span><span></span><span></span></div></div>'
		+ '</div>';
	$('#chatArea').append(html);
	scrollToBottom();
}

function hideTyping() { $('#typingIndicator').remove(); }

/* 构建RAG来源标签（简单标签，非引用卡片） */
function buildRagSourceTag() {
	var kbNames = [];
	for (var ki = 0; ki < selectedKbIds.length; ki++) {
		var rkb = kbCache[selectedKbIds[ki]];
		kbNames.push(rkb ? rkb.kbName : '知识库');
	}
	return '<div class="rag-source-tag"><i class="fa fa-book"></i> 引用知识库：' + escapeHtml(kbNames.join('、')) + '</div>';
}

// ==================== 消息发送 ====================

/* 发送消息（后端惰性创建会话） */
function sendMessage() {
	var msg = $('#userInput').val().trim();
	var fileText = (uploadedFileInfo && uploadedFileInfo.text) ? uploadedFileInfo.text : null;

	if ((!msg && !fileText) || isProcessing) return;

	$('#welcomePage').remove();

	// 构造显示消息
	var displayMsg = msg;
	if (fileText && msg) {
		displayMsg = msg + '\n\n[附件：' + (uploadedFileInfo.fileName || '文件') + ']';
	} else if (fileText && !msg) {
		displayMsg = '[附件内容：' + (uploadedFileInfo.fileName || '文件') + ']\n\n' + (fileText.length > 200 ? fileText.substring(0, 200) + '...' : fileText);
	}
	appendMessage('user', displayMsg);

	// 构造实际发送的内容
	var sendContent = '';
	if (fileText) {
		sendContent = fileText;
		if (msg) sendContent = msg + '\n\n--- 附件内容 ---\n' + fileText;
	} else {
		sendContent = msg;
	}

	// 清空输入框和上传文件
	$('#userInput').val('');
	if (uploadedFileInfo) {
		uploadedFileInfo = null;
		$('#fileInfo').html('(支持 PDF/Word/Excel/图片/TXT，最大20MB)');
		$('#fileUpload').val('');
	}

	// 保存为需求文档（流程使用）
	if (!analysisHistory['1.1'] && sendContent.length > 50) {
		analysisHistory['1.1'] = sendContent;
	}

	// 选择流式或普通模式（长消息自动降级POST）
	if (useStreamMode && sendContent.length <= 2000) {
		callAiChatStream(sendContent);
	} else {
		callAiChat(sendContent);
	}
}

/* 键盘事件 */
function handleKeyDown(e) {
	if (e.key === 'Enter' && !e.shiftKey) {
		e.preventDefault();
		sendMessage();
	}
}
