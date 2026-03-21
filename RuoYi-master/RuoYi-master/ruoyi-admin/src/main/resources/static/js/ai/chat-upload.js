/**
 * AI开发助手 - 文件上传模块
 * handleFileUpload, clearFile
 */

function handleFileUpload(input) {
	var file = input.files[0];
	if (!file) return;

	if (file.size > 20 * 1024 * 1024) {
		layer.msg('文件大小不能超过20MB', {icon: 5});
		input.value = '';
		return;
	}

	var ext = file.name.split('.').pop().toLowerCase();
	var allowed = ['pdf','doc','docx','xls','xlsx','txt','md','png','jpg','jpeg','gif','bmp','webp'];
	if (allowed.indexOf(ext) === -1) {
		layer.msg('不支持的文件格式：' + ext, {icon: 5});
		input.value = '';
		return;
	}

	$('#fileInfo').html('<i class="fa fa-spinner fa-spin"></i> 正在解析 ' + file.name + ' ...');
	$('#btnSend').prop('disabled', true);

	var formData = new FormData();
	formData.append('file', file);

	$.ajax({
		url: prefix + '/upload', type: 'POST',
		data: formData, processData: false, contentType: false, timeout: 120000,
		success: function(res) {
			if (res.code == 0) {
				var data = res.data;
				var text = data.text;
				var fileName = data.fileName || file.name;
				var storedName = data.storedName;
				var fileSize = data.fileSize || file.size;

				uploadedFileInfo = { fileName: fileName, storedName: storedName, fileSize: fileSize, text: text };
				analysisHistory['1.1'] = text;

				var downloadUrl = prefix + '/download?storedName=' + encodeURIComponent(storedName) + '&fileName=' + encodeURIComponent(fileName);
				appendMessage('ai', '文件 "' + fileName + '" 解析成功，共 ' + text.length + ' 字符。\n\n文件已就绪，请在输入框中输入问题后点击「发送」开始对话，或点击快捷按钮进行需求分析。');

				$('#fileInfo').html('<a href="' + downloadUrl + '" style="color:#1ab394;" title="点击下载"><i class="fa fa-file"></i> ' + fileName + ' (' + formatFileSize(fileSize) + ')</a>'
					+ ' <span class="remove-file" onclick="clearFile()"><i class="fa fa-times"></i></span>');
			} else {
				appendMessage('ai', '文件解析失败：' + res.msg);
				$('#fileInfo').html('(支持 PDF/Word/Excel/图片/TXT，最大20MB)');
				uploadedFileInfo = null;
			}
		},
		error: function(xhr, status, error) {
			appendMessage('ai', '文件上传失败：' + (status === 'timeout' ? '请求超时' : error));
			$('#fileInfo').html('(支持 PDF/Word/Excel/图片/TXT，最大20MB)');
			uploadedFileInfo = null;
		},
		complete: function() {
			$('#btnSend').prop('disabled', false);
			input.value = '';
		}
	});
}

function clearFile() {
	delete analysisHistory['1.1'];
	uploadedFileInfo = null;
	$('#fileInfo').html('(支持 PDF/Word/Excel/图片/TXT，最大20MB)');
	$('#fileUpload').val('');
	layer.msg('已清除', {icon: 1});
}
