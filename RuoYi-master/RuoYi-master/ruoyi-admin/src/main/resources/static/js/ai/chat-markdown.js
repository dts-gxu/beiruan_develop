/**
 * AI开发助手 - Markdown渲染模块
 * formatMarkdown, formatTables, trimIncompleteTable
 */

function formatMarkdown(text) {
	if (!text) return '';

	// 1. 提取代码块，用占位符保护
	var codeBlocks = [];
	text = text.replace(/```(\w*)\n([\s\S]*?)```/g, function(m, lang, code) {
		var idx = codeBlocks.length;
		codeBlocks.push('<pre><code>' + code.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</code></pre>');
		return '%%CODEBLOCK_' + idx + '%%';
	});

	// 2. 提取行内代码
	var inlineCodes = [];
	text = text.replace(/`([^`]+)`/g, function(m, code) {
		var idx = inlineCodes.length;
		inlineCodes.push('<code>' + code.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</code>');
		return '%%INLINE_' + idx + '%%';
	});

	// 3. 将AI可能输出的<br>/<br/>标签统一为换行符
	text = text.replace(/<br\s*\/?>/gi, '\n');

	// 4. 转义HTML（代码块和行内代码已被占位符保护）
	text = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

	// 5. Markdown语法转换
	text = text.replace(/\*\*(.+?)\*\*/g, '<b>$1</b>');
	text = text.replace(/\*(.+?)\*/g, '<i>$1</i>');
	text = text.replace(/^#{3}\s+(.+)$/gm, '<h5 style="margin:8px 0 4px;font-size:14px;">$1</h5>');
	text = text.replace(/^#{2}\s+(.+)$/gm, '<h4 style="margin:10px 0 6px;font-size:15px;">$1</h4>');
	text = text.replace(/^#{1}\s+(.+)$/gm, '<h3 style="margin:12px 0 8px;">$1</h3>');
	text = text.replace(/^---+$/gm, '<hr style="margin:8px 0;border:none;border-top:1px solid #ddd;">');
	text = text.replace(/^\d+\.\s+(.+)$/gm, function(m, content) { return '<div style="margin-left:16px;">' + m + '</div>'; });
	text = text.replace(/^[-*]\s+(.+)$/gm, '<div style="margin-left:16px;">• $1</div>');
	// 知识库引用标记高亮：【来源：xxx】
	text = text.replace(/【来源[：:](.+?)】/g, '<span class="rag-cite-mark">📖 $1</span>');

	// 6. 表格处理（生成的HTML用占位符保护，避免被<br>破坏）
	var tableBlocks = [];
	text = formatTables(text);
	text = text.replace(/<div class="table-wrap">[\s\S]*?<\/table><\/div>/g, function(m) {
		var idx = tableBlocks.length;
		tableBlocks.push(m);
		return '%%TABLE_' + idx + '%%';
	});

	// 7. 普通换行转<br>（表格/代码块已被占位符保护）
	text = text.replace(/\n/g, '<br>');
	text = text.replace(/(<br>){3,}/g, '<br><br>');

	// 8. 还原代码块、行内代码、表格
	for (var i = 0; i < codeBlocks.length; i++) {
		text = text.replace('%%CODEBLOCK_' + i + '%%', codeBlocks[i]);
	}
	for (var i = 0; i < inlineCodes.length; i++) {
		text = text.replace('%%INLINE_' + i + '%%', inlineCodes[i]);
	}
	for (var i = 0; i < tableBlocks.length; i++) {
		text = text.replace('%%TABLE_' + i + '%%', tableBlocks[i]);
	}

	return text;
}

// 流式渲染时裁剪不完整的表格行，防止表格抖动
function trimIncompleteTable(text) {
	if (!text) return text;
	var lines = text.split('\n');
	while (lines.length > 0) {
		var last = lines[lines.length - 1].trim();
		if (last.length > 0 && last.charAt(0) === '|' && last.charAt(last.length - 1) !== '|') {
			lines.pop();
		} else {
			break;
		}
	}
	return lines.join('\n');
}

function formatTables(text) {
	var lines = text.split('\n');
	var result = [];
	var inTable = false;
	var tableDataRows = [];
	var tableHeaderRow = null;
	var hasSeparator = false;

	function flushTable() {
		if (!tableHeaderRow && tableDataRows.length === 0) return;
		var maxCols = tableHeaderRow ? tableHeaderRow.length : 0;
		for (var r = 0; r < tableDataRows.length; r++) {
			if (tableDataRows[r].length > maxCols) maxCols = tableDataRows[r].length;
		}
		if (maxCols === 0) return;
		var html = '<div class="table-wrap"><table>';
		if (tableHeaderRow) {
			html += '<thead><tr>';
			for (var h = 0; h < maxCols; h++) {
				html += '<th>' + (h < tableHeaderRow.length ? tableHeaderRow[h] : '') + '</th>';
			}
			html += '</tr></thead>';
		}
		html += '<tbody>';
		for (var r = 0; r < tableDataRows.length; r++) {
			html += '<tr>';
			for (var c = 0; c < maxCols; c++) {
				html += '<td>' + (c < tableDataRows[r].length ? tableDataRows[r][c] : '') + '</td>';
			}
			html += '</tr>';
		}
		html += '</tbody></table></div>';
		result.push(html);
	}

	function parseCells(line) {
		var inner = line.replace(/^\|/, '').replace(/\|$/, '');
		var parts = inner.split('|');
		var cells = [];
		for (var k = 0; k < parts.length; k++) {
			cells.push(parts[k].trim());
		}
		return cells;
	}

	for (var i = 0; i < lines.length; i++) {
		var line = lines[i].trim();
		if (line.match(/^\|.*\|$/)) {
			if (line.match(/^\|[\s\-:|]+\|$/)) {
				hasSeparator = true;
				continue;
			}
			if (!inTable) {
				inTable = true;
				tableHeaderRow = null;
				tableDataRows = [];
				hasSeparator = false;
				tableHeaderRow = parseCells(line);
			} else {
				tableDataRows.push(parseCells(line));
			}
		} else {
			if (inTable) {
				flushTable();
				inTable = false;
				tableHeaderRow = null;
				tableDataRows = [];
				hasSeparator = false;
			}
			result.push(line);
		}
	}
	if (inTable) {
		flushTable();
	}
	return result.join('\n');
}
