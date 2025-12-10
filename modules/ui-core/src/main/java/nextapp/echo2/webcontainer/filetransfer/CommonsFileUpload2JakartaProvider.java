/*
 * $Id: $
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package nextapp.echo2.webcontainer.filetransfer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import nextapp.echo2.app.filetransfer.UploadSelect;
import nextapp.echo2.webrender.Connection;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.JakartaServletDiskFileUpload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Multipart upload provider that bridges Echo2's legacy expectations to commons-fileupload 2.x.
 */
public class CommonsFileUpload2JakartaProvider extends AbstractFileUploadProvider {

    private static final String UPLOAD_FIELD_NAME = "Echo.UploadForm.File";

    @Override
    public void updateComponent(final Connection connection, final UploadSelect uploadSelect)
            throws IOException, ServletException {
        final HttpServletRequest request = getWrappedRequest(connection.getRequest());

        final JakartaServletDiskFileUpload upload = new JakartaServletDiskFileUpload(buildFileItemFactory());
        final int sizeLimit = getFileUploadSizeLimit();
        if (sizeLimit > 0) {
            final long limit = sizeLimit;
            upload.setSizeMax(limit);
            upload.setFileSizeMax(limit);
        }

        final List<DiskFileItem> items;
        try {
            items = upload.parseRequest(request);
        } catch (final FileUploadException e) {
            throw new IOException(e.getMessage(), e);
        }

        for (final DiskFileItem item : items) {
            if (!UPLOAD_FIELD_NAME.equals(item.getFieldName())) {
                continue;
            }

            try (InputStream inputStream = item.getInputStream()) {
                final File tempFile = writeTempFile(inputStream, uploadSelect);
                final int reportedSize = (int) Math.min(Integer.MAX_VALUE, item.getSize());
                final UploadEvent uploadEvent = new UploadEvent(
                        tempFile,
                        reportedSize,
                        item.getContentType(),
                        item.getName()
                );
                UploadSelectPeer.activateUploadSelect(uploadSelect, uploadEvent);
            } finally {
                try {
                    item.delete();
                } catch (final IOException ex) {
                    // Ignore cleanup failures; the temporary files will be GC-cleaned.
                }
            }
            break;
        }
    }

    private DiskFileItemFactory buildFileItemFactory() throws IOException {
        final DiskFileItemFactory.Builder builder = DiskFileItemFactory.builder();
        builder.setBufferSize(getMemoryCacheThreshold());
        builder.setCharset(StandardCharsets.UTF_8);

        final File cacheLocation = getDiskCacheLocation();
        if (cacheLocation != null) {
            final Path repository = cacheLocation.toPath();
            Files.createDirectories(repository);
            builder.setPath(repository);
        }

        return builder.get();
    }
}
