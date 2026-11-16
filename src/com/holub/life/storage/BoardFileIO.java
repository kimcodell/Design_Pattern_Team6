package com.holub.life.storage;

import java.io.*;
import java.util.*;
import com.holub.life.model.BoardSnapshot;
import com.holub.life.storage.format.BoardSerializer;

/** Reads and writes board files. Picks the format by file extension
 *  from the formats it was given; it does not know how any format
 *  works. Adding a format means writing a {@link BoardSerializer} and
 *  registering it where this object is created, not changing this class.
 */
public final class BoardFileIO
{
	private final Map<String, BoardSerializer> serializers = new LinkedHashMap<>();

	public BoardFileIO(List<BoardSerializer> formats)
	{
		if (formats.isEmpty())
			throw new IllegalArgumentException("at least one format is required");
		for (BoardSerializer format : formats)
			if (serializers.putIfAbsent(format.extension(), format) != null)
				throw new IllegalArgumentException("two formats for " + format.extension());
	}

	/** Extensions of the registered formats, in registration order. */
	public List<String> extensions()
	{
		return List.copyOf(serializers.keySet());
	}

	public BoardSnapshot read(File file) throws IOException
	{
		BoardSerializer format = formatFor(file);
		try (InputStream in = new BufferedInputStream(new FileInputStream(file)))
		{
			return format.read(in);
		}
	}

	public void write(File file, BoardSnapshot snapshot) throws IOException
	{
		BoardSerializer format = formatFor(file);
		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file)))
		{
			format.write(snapshot, out);
		}
	}

	private BoardSerializer formatFor(File file) throws IOException
	{
		for (Map.Entry<String, BoardSerializer> entry : serializers.entrySet())
			if (file.getName().endsWith(entry.getKey()))
				return entry.getValue();
		throw new IOException("Unknown file type: " + file.getName()
							  + " (supported: " + extensions() + ")");
	}
}
