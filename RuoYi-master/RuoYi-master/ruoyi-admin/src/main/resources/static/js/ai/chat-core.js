/**
 * AI开发助手 - 核心模块
 * 全局变量、初始化、工具函数、滚动控制
 */

var prefix = ctx + "ai/assistant";
var conversations = [];           // 会话列表缓存
var currentConversationId = null;  // 当前会话ID
var isProcessing = false;
var uploadedFileInfo = null;       // {fileName, storedName, fileSize, text}
var analysisHistory = {};          // 流程步骤缓存
var useStreamMode = true;          // 是否使用SSE流式模式
var messageCache = {};             // 每个会话的消息DOM缓存
var selectedKbIds = [];            // 当前选中的知识库ID数组（支持多选）
var kbCache = {};                  // 缓存知识库元数据 { kbId: {kbName, kbDesc, documentCount, ...} }
var lastQuotes = [];               // 最近一次RAG检索的引用段落

// ==================== 初始化 ====================

$(function() {
	loadConversations();
	loadKnowledgeBases();
	$('#userInput').focus();
	// 从 sessionStorage 恢复上次的会话ID
	var savedId = sessionStorage.getItem('ai_currentConvId');
	if (savedId) {
		currentConversationId = parseInt(savedId);
	}
	// 恢复知识库选择（多选）
	var savedKbs = sessionStorage.getItem('ai_selectedKbIds');
	if (savedKbs) {
		try { selectedKbIds = JSON.parse(savedKbs); } catch(e) { selectedKbIds = []; }
	}
});

// ==================== 消息缓存（仿FastGPT chatRecords） ====================

/* 保存当前聊天区域DOM到缓存 */
function saveCurrentChatToCache() {
	if (currentConversationId && $('#chatArea .chat-message').length > 0) {
		messageCache[currentConversationId] = $('#chatArea').html();
	}
}

/* 从缓存恢复聊天区域 */
function restoreFromCache(conversationId) {
	if (messageCache[conversationId]) {
		$('#chatArea').html(messageCache[conversationId]);
		scrollToBottom();
		return true;
	}
	return false;
}

/* 乐观更新侧栏中指定会话的标题和消息数 */
function onUpdateHistoryTitle(conversationId, newTitle) {
	var $item = $('.conv-item[data-id="' + conversationId + '"]');
	if ($item.length) {
		$item.find('.conv-title').contents().first().replaceWith(escapeHtml(newTitle));
		var curCount = parseInt($item.find('.conv-meta').text()) || 0;
		$item.find('.conv-meta').text((curCount + 2) + '条消息 · 刚刚');
	} else {
		loadConversations();
	}
}

// ==================== 滚动控制 ====================

var userScrolledUp = false;
$('#chatArea').on('scroll', function() {
	var area = this;
	userScrolledUp = (area.scrollHeight - area.scrollTop - area.clientHeight) > 50;
});

function scrollToBottom(force) {
	if (!force && userScrolledUp) return;
	var area = document.getElementById('chatArea');
	area.scrollTo({ top: area.scrollHeight, behavior: 'auto' });
}

// ==================== 工具函数 ====================

function formatFileSize(bytes) {
	if (bytes < 1024) return bytes + 'B';
	if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB';
	return (bytes / (1024 * 1024)).toFixed(1) + 'MB';
}

function escapeHtml(str) {
	if (!str) return '';
	return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
