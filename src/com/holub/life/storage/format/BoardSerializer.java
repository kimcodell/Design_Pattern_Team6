package com.holub.life.storage.format;

import java.io.*;
import com.holub.life.model.BoardSnapshot;

/** A file format for boards (Strategy). Each format turns a snapshot
 *  into bytes and back. A serializer checks the file's syntax; whether
 *  the cells fit the board is checked by the model when it restores.
 */
public interface BoardSerializer
{
	void write(BoardSnapshot snapshot, OutputStream out) throws IOException;

	BoardSnapshot read(InputStream in) throws IOException;

	/** File name extension this format handles, including the dot (".life"). */
	String extension();
}
