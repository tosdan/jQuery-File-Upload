
/*jslint unparam: true, regexp: true */
/*global window, $ */
$(function () {
    'use strict';
    // Change this to the location of your server-side upload handler:
    var url = '/jQuery-File-Upload/servlet/ajaxupload/up',
        uploadButton = $('<button id="upload"/>')
            .addClass('btn btn-xs btn-primary right-spacing')
            .prop('disabled', true)
            .text('Processing...')
            .on('click', function () {
                var $this = $(this),
                    data = $this.data();
                $this
                    .off('click')
                    .text('Annulla')
                    .removeClass('btn-primary')
                    .addClass('btn-warning')
                    .on('click', function () {
                        $this.remove();
                        data.abort();
                    })
                    .siblings('#cancel').remove();
                
                data.submit().always(function () {
                    $this.remove();
                });
            }),
            
        deleteButton = $('<button id="delete" />')
        	.addClass("btn btn-xs btn-danger right-spacing")
        	.text('Elimina')
        	.hide()
        	.on('click', function () {
        		var $this = $(this),
        			deleteUrl = $this.data('deleteUrl'),
        			thisFilename = $this.data().files[0].name;
//        		console.log(thisFilename);
        		if (deleteUrl) {
        			$.get(deleteUrl, function (data) {
        				data = $.parseJSON(data);
//                		console.log(data.files[0][thisFilename]);
        				if (data.files[0].hasOwnProperty(thisFilename)) {
//        					console.log('cancellato');
        					var span = $('<span>')
        							.addClass('text-danger')
        							.text('File "' + thisFilename + '" rimosso.');
        					$this.closest('div')
        						.empty()
        						.append(span);
        				}
        			});
        		}
        	}),
            
        cancelButton = $('<button id="cancel" />')
        	.addClass("btn btn-xs btn-warning right-spacing")
        	.text('Annulla')
//            .prop('disabled', true) // va modificato insieme a #C001
        	.on('click', function () {
        		$(this).closest('div').remove();
        	}),
        	
        progressBar = $('<div id=progress />')
        	.addClass('progress')
        	.append('<div class="progress-bar progress-bar-success"></div>');
    
    $('#fileupload').fileupload({
        url: url,
        dataType: 'json',
        autoUpload: false,
//        acceptFileTypes: /(\.|\/)(gif|jpe?g|png)$/i,
        maxFileSize: 5000000, // 5 MB
        // Enable image resizing, except for Android and Opera,
        // which actually support image resizing, but fail to
        // send Blob objects via XHR requests:
        disableImageResize: /Android(?!.*Chrome)|Opera/
            .test(window.navigator.userAgent),
        previewMaxWidth: 100,
        previewMaxHeight: 100,
        previewCrop: true
        
    }).on('fileuploadadd', function (e, data) {
        data.context = $('<div/>').appendTo('#files');
        $.each(data.files, function (index, file) {
            var node = $('<p/>')
            		.addClass('right-spacing')
                    .append( $('<span/>').addClass('file-name').text(file.name) );
            if (!index) {
                node
//                     .append('<br>')
                	.append(progressBar.clone(true).data(data))
                    .append(uploadButton.clone(true).data(data))
                    .append(cancelButton.clone(true).data(data))
                    .append(deleteButton.clone(true).data(data));
            }
            node.appendTo(data.context);
        });
        
    }).on('fileuploadprocessalways', function (e, data) {
        var index = data.index,
            file = data.files[index],
            node = $(data.context.children()[index]);
        /*
        if (file.preview) {
            node
                .prepend('<br>')
                .prepend(file.preview) 
                ;
        }
        */
        if (file.error) {
            node
                .append('<br>')
                .append($('<span class="text-danger"/>').text(file.error));
        }
        if (index + 1 === data.files.length) {
            data.context.find('button#upload')
                .text('Upload')
                .prop('disabled', !!data.files.error);
            
//            data.context.find('button#cancel').prop('disabled', !!data.files.error); // va modificato insieme a #C001
        }
        
    }).on('fileuploadprogressall', function (e, data) {
        var progress = parseInt(data.loaded / data.total * 100, 10);
        $('#progress .progress-bar').css(
            'width',
            progress + '%'
        );
        
    }).on('fileuploaddone', function (e, data) {
//    	console.log(data);
        $.each(data.result.files, function (index, file) {
    		var fileSection = $(data.context.children()[index]),
			deleteButton = $('#delete', fileSection),
			uploadButton = $('#upload', fileSection);
    		
        	if (file.deleteUrl) {
        		deleteButton.data('deleteUrl', file.deleteUrl)
        					.show();
        	}
        	
            if (file.url) {
                var link = $('<a>')
                    .attr('target', '_blank')
                    .prop('href', file.url);
                fileSection
                    .wrap(link);
            } else if (file.error) {
                var error = $('<span class="text-danger"/>').text(file.error);
                fileSection
                    .append('<br>')
                    .append(error);
            }
        });
        
    }).on('fileuploadfail', function (e, data) {
        $.each(data.files, function (index, file) {
            var error = $('<span class="text-danger"/>').text('File upload failed.');
            $(data.context.children()[index])
                .append('<br>')
                .append(error);
        });
        
    }).prop('disabled', !$.support.fileInput)
        .parent().addClass($.support.fileInput ? undefined : 'disabled');
});