<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>Prove</title>

<link rel="stylesheet" href="css/bootstrap.css" />

<script src="js/jquery-1.8.2.min.js"></script>
<script src="js/vendor/jquery.ui.widget.js"></script>
<script src="js/jquery.iframe-transport.js"></script>
<script src="js/jquery.fileupload.js"></script>

<script>
$(function () {
    $('#fileupload').fileupload({
        dataType: 'json',
        add: function (e, data) {
        	var $button = $('<button/>').text('Upload');
            data.context = $('<div/>').html( $button )
                .appendTo('#lista')
                .on('click', function () {
                	console.log(this);
                    data.context = $('<p/>').text('Uploading...').replaceAll( $button );
                    data.submit();
                });
            // Questo metodo funziona bene.
            $('#carica').on('click', function() {
            	data.submit();
            	data.context.unbind('click');
            	console.log(data.files);
            	console.log(data.fileInput);
            });
        },
        done: function (e, data) {
            data.context.text('Upload finished.');
            data.files.splice(0,1);
        }
    });
});
</script>

</head>
<body>
<form  id="fileupload" class="well" method="POST" enctype="multipart/form-data" action="${ pageContext.request.contextPath }/servlet/ajaxupload/">
	<input id="fileuploadInput" type="file" name="files[]" data-url="${ pageContext.request.contextPath }/servlet/ajaxupload/" multiple/>
	<div id="lista"></div>
	<button id="carica" type="button" class="btn btn-primary start">Carica</button>
</form>
</body>
</html>