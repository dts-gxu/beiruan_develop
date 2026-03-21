/**
 * AI开发助手 - 会话管理模块
 * 左侧会话列表的CRUD操作
 */

// ==================== 会话列表管理 ====================

/* 加载当前用户的所有会话 */
function loadConversations() {
	$.ajax({
		url: prefix + '/conversation/list',
		type: 'POST',
		data: {},
		success: function(res) {
			if (res.code == 0) {
				conversations = res.data || [];
				renderConvList(conversations);
				if (currentConversationId && !$('#chatArea .chat-message').length) {
					var found = conversations.find(function(c) { return c.conversationId == currentConversationId; });
					if (found) {
						switchConversation(currentConversationId);
					} else {
						currentConversationId = null;
						sessionStorage.removeItem('ai_currentConvId');
					}
				}
			}
		}
	});
}

/* 渲染左侧会话列表 */
function renderConvList(list) {
	var container = $('#convList');
	container.empty();
	if (!list || list.length === 0) {
		container.html('<div class="conv-empty"><i class="fa fa-comments" style="font-size:24px;display:block;margin-bottom:10px;"></i>暂无对话记录<br/>点击上方按钮开始新对话</div>');
		return;
	}
	for (var i = 0; i < list.length; i++) {
		var c = list[i];
		var isActive = (currentConversationId == c.conversationId);
		var isPinned = (c.top && c.top > 0);
		var time = c.lastMessageTime || c.createTime || '';
		if (time) {
			try { time = time.substring(5, 16); } catch(e) {}
		}
		var msgCount = c.messageCount || 0;
		var displayTitle = c.customTitle || c.title || '新对话';
		var pinIcon = isPinned ? '<i class="fa fa-thumb-tack" style="color:#f9e2af;margin-left:4px;font-size:10px;"></i>' : '';
		var pinAction = isPinned
			? '<a class="pin" href="javascript:void(0)" onclick="event.stopPropagation();togglePinConversation(' + c.conversationId + ',0)" title="取消置顶"><i class="fa fa-thumb-tack"></i></a>'
			: '<a class="pin" href="javascript:void(0)" onclick="event.stopPropagation();togglePinConversation(' + c.conversationId + ',1)" title="置顶"><i class="fa fa-thumb-tack"></i></a>';
		var classes = 'conv-item' + (isActive ? ' active' : '') + (isPinned ? ' pinned' : '');
		var html = '<div class="' + classes + '" data-id="' + c.conversationId + '" onclick="switchConversation(' + c.conversationId + ')">' 
			+ '<div class="conv-icon"><i class="fa fa-comment"></i></div>'
			+ '<div class="conv-info">'
			+ '<div class="conv-title">' + escapeHtml(displayTitle) + pinIcon + '</div>'
			+ '<div class="conv-meta">' + msgCount + '条消息 · ' + time + '</div>'
			+ '</div>'
			+ '<div class="conv-actions">'
			+ pinAction
			+ '<a class="edit" href="javascript:void(0)" onclick="event.stopPropagation();renameConversation(' + c.conversationId + ')" title="重命名"><i class="fa fa-pencil"></i></a>'
			+ '<a href="javascript:void(0)" onclick="event.stopPropagation();deleteConversation(' + c.conversationId + ')" title="删除"><i class="fa fa-trash"></i></a>'
			+ '</div></div>';
		container.append(html);
	}
}

/* 搜索过滤会话 */
function filterConversations() {
	var keyword = $('#convSearch').val().trim().toLowerCase();
	if (!keyword) {
		renderConvList(conversations);
		return;
	}
	var filtered = conversations.filter(function(c) {
		return (c.title || '').toLowerCase().indexOf(keyword) >= 0
			|| (c.customTitle || '').toLowerCase().indexOf(keyword) >= 0;
	});
	renderConvList(filtered);
}

/* 新建对话 */
function createNewChat() {
	saveCurrentChatToCache();
	currentConversationId = null;
	sessionStorage.removeItem('ai_currentConvId');
	$('#chatArea').empty();
	$('#chatArea').html('<div class="welcome-page" id="welcomePage"><div class="welcome-icon"><i class="fa fa-robot"></i></div><h3>新对话</h3><p>请直接输入问题开始对话。<br/>第一条消息将自动成为对话标题。</p></div>');
	$('#currentTitle').html('<i class="fa fa-comments"></i> 新对话');
	analysisHistory = {};
	uploadedFileInfo = null;
	$('#fileInfo').html('(支持 PDF/Word/Excel/图片/TXT，最大20MB)');
	$('#fileUpload').val('');
	$('.conv-item').removeClass('active');
	$('#userInput').focus();
}

/* 切换会话 */
function switchConversation(conversationId) {
	if (isProcessing) return;
	if (currentConversationId == conversationId) return;
	saveCurrentChatToCache();
	currentConversationId = conversationId;
	sessionStorage.setItem('ai_currentConvId', conversationId);
	$('.conv-item').removeClass('active');
	$('.conv-item[data-id="' + conversationId + '"]').addClass('active');
	var conv = conversations.find(function(c) { return c.conversationId == conversationId; });
	if (conv) {
		$('#currentTitle').html('<i class="fa fa-comments"></i> ' + escapeHtml(conv.customTitle || conv.title || '新对话'));
	}
	analysisHistory = {};
	uploadedFileInfo = null;
	$('#fileInfo').html('(支持 PDF/Word/Excel/图片/TXT，最大20MB)');
	$('#fileUpload').val('');
	if (!restoreFromCache(conversationId)) {
		loadMessages(conversationId);
	}
}

/* 加载会话消息 */
function loadMessages(conversationId) {
	$('#chatArea').empty();
	$('#welcomePage').remove();
	showTyping();
	$.ajax({
		url: prefix + '/conversation/messages',
		type: 'POST',
		data: { conversationId: conversationId },
		success: function(res) {
			hideTyping();
			$('#chatArea').empty();
			if (res.code == 0 && res.data && res.data.length > 0) {
				var msgs = res.data;
				var stepNameMap = {
					'关键信息提取': '1.3', 'RuoYi适配分析': '1.4',
					'完整性评估': '1.5', '完整性报告': '1.6'
				};
				var lastUserStepKey = null;
				for (var i = 0; i < msgs.length; i++) {
					var role = msgs[i].role === '0' ? 'user' : 'ai';
					var content = msgs[i].content || '';
					appendMessage(role, content);
					if (role === 'user') {
						lastUserStepKey = null;
						for (var name in stepNameMap) {
							if (content.indexOf('[' + name + ']') === 0) {
								lastUserStepKey = stepNameMap[name];
								break;
							}
						}
					} else if (role === 'ai' && lastUserStepKey && content.length > 0) {
						analysisHistory[lastUserStepKey] = content;
						lastUserStepKey = null;
					}
				}
				if (analysisHistory['1.6']) {
					appendHtmlMessage('ai', '<b>全流程分析已完成</b><br><br><button class="btn btn-sm btn-success" onclick="downloadWordReport()" style="margin-top:5px;"><i class="fa fa-download"></i> 下载Word报告</button>');
				}
				messageCache[conversationId] = $('#chatArea').html();
			} else {
				appendMessage('ai', '会话已就绪，请输入消息开始对话。');
			}
		},
		error: function() {
			hideTyping();
			appendMessage('ai', '加载消息失败，请重试。');
		}
	});
}

/* 删除会话 */
function deleteConversation(conversationId) {
	layer.confirm('确认删除该会话？所有消息将被清除。', {icon: 3, title: '提示'}, function(index) {
		$.ajax({
			url: prefix + '/conversation/delete',
			type: 'POST',
			data: { conversationId: conversationId },
			success: function(res) {
				if (res.code == 0) {
					layer.msg('删除成功', {icon: 1});
					delete messageCache[conversationId];
					if (currentConversationId == conversationId) {
						currentConversationId = null;
						sessionStorage.removeItem('ai_currentConvId');
						$('#chatArea').empty();
						$('#chatArea').html('<div class="welcome-page" id="welcomePage"><div class="welcome-icon"><i class="fa fa-robot"></i></div><h3>欢迎使用 AI 开发助手</h3><p>点击左侧 <b>「新建对话」</b> 开始新的会话。</p></div>');
						$('#currentTitle').html('<i class="fa fa-comments"></i> AI开发助手');
					}
					loadConversations();
				} else {
					layer.msg(res.msg || '删除失败', {icon: 5});
				}
			}
		});
		layer.close(index);
	});
}

/* 重命名会话 */
function renameConversation(conversationId) {
	var conv = conversations.find(function(c) { return c.conversationId == conversationId; });
	var currentTitle = conv ? (conv.customTitle || conv.title || '') : '';
	layer.prompt({title: '修改会话标题', formType: 0, value: currentTitle}, function(value, index) {
		if (!value || !value.trim()) return;
		$.ajax({
			url: prefix + '/conversation/rename',
			type: 'POST',
			data: { conversationId: conversationId, title: value.trim() },
			success: function(res) {
				if (res.code == 0) {
					layer.msg('修改成功', {icon: 1});
					if (currentConversationId == conversationId) {
						$('#currentTitle').html('<i class="fa fa-comments"></i> ' + escapeHtml(value.trim()));
					}
					loadConversations();
				} else {
					layer.msg(res.msg || '修改失败', {icon: 5});
				}
			}
		});
		layer.close(index);
	});
}

/* 切换置顶 */
function togglePinConversation(conversationId, top) {
	$.ajax({
		url: prefix + '/conversation/toggleTop',
		type: 'POST',
		data: { conversationId: conversationId, top: top },
		success: function(res) {
			if (res.code == 0) {
				layer.msg(top ? '已置顶' : '已取消置顶', {icon: 1});
				loadConversations();
			} else {
				layer.msg(res.msg || '操作失败', {icon: 5});
			}
		}
	});
}

/* 清空所有对话历史 */
function clearAllConversations() {
	if (conversations.length === 0) {
		layer.msg('暂无对话可清空', {icon: 5});
		return;
	}
	layer.confirm('确认清空所有对话历史？此操作不可恢复。', {icon: 3, title: '警告'}, function(index) {
		$.ajax({
			url: prefix + '/conversation/clearAll',
			type: 'POST',
			success: function(res) {
				if (res.code == 0) {
					layer.msg('已清空所有对话', {icon: 1});
					currentConversationId = null;
					sessionStorage.removeItem('ai_currentConvId');
					$('#chatArea').empty();
					$('#chatArea').html('<div class="welcome-page" id="welcomePage"><div class="welcome-icon"><i class="fa fa-robot"></i></div><h3>欢迎使用 AI 开发助手</h3><p>所有对话已清空。<br/>点击左侧 <b>「新建对话」</b> 开始新的会话。</p></div>');
					$('#currentTitle').html('<i class="fa fa-comments"></i> AI开发助手');
					analysisHistory = {};
					uploadedFileInfo = null;
					messageCache = {};
					loadConversations();
				} else {
					layer.msg(res.msg || '清空失败', {icon: 5});
				}
			}
		});
		layer.close(index);
	});
}
