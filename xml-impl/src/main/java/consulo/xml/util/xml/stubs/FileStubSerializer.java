/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.xml.util.xml.stubs;

import consulo.language.psi.stub.*;
import consulo.util.xml.fastReader.XmlFileHeader;

import jakarta.annotation.Nonnull;
import java.io.IOException;

/**
 * @author Dmitry Avdeev
 *         Date: 8/8/12
 */
public class FileStubSerializer implements ObjectStubSerializer<FileStub, Stub>
{
	@Nonnull
	@Override
	public String getExternalId()
	{
		return "xml.FileStubSerializer";
	}

	@Override
	public void serialize(@Nonnull FileStub stub, @Nonnull StubOutputStream dataStream) throws IOException
	{
		XmlFileHeader header = stub.getHeader();
		dataStream.writeName(header.getRootTagLocalName());
		dataStream.writeName(header.getRootTagNamespace());
		dataStream.writeName(header.getPublicId());
		dataStream.writeName(header.getSystemId());
	}

	@Nonnull
	@Override
	public FileStub deserialize(@Nonnull StubInputStream dataStream, Stub parentStub) throws IOException
	{
		return new FileStub(dataStream.readName(), dataStream.readName(), dataStream.readName(), dataStream.readName());
	}

	@Override
	public void indexStub(@Nonnull FileStub stub, @Nonnull IndexSink sink)
	{
	}

	@Override
	public String toString()
	{
		return "File";
	}
}
