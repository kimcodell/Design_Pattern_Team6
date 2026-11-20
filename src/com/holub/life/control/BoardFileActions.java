package com.holub.life.control;

import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.JOptionPane;
import com.holub.io.Files;
import com.holub.life.model.LifeModel;
import com.holub.life.storage.BoardFileIO;

/**
 * Coordinates the Load and Store UI actions on the Swing event-dispatch thread.
 * File selection and error dialogs belong here; BoardFileIO handles files and
 * serializers handle their formats. The clock stops after a file is selected,
 * preserving the existing cancellation and timing behavior.
 */
public final class BoardFileActions
{
	private final Function<String, File> fileChooser;
	private final Consumer<String> errorReporter;
	private final LifeModel model;
	private final BoardFileIO files;
	private final Clock clock;

	public BoardFileActions(Component parent, LifeModel model, BoardFileIO files, Clock clock)
	{
		this(model, files, clock, BoardFileActions::chooseFile,
				message -> JOptionPane.showMessageDialog(parent, message,
						"The Game of Life", JOptionPane.ERROR_MESSAGE));
	}

	/** Allows the UI interactions to be replaced for headless tests. */
	BoardFileActions(LifeModel model, BoardFileIO files, Clock clock,
			Function<String, File> fileChooser, Consumer<String> errorReporter)
	{
		this.model = Objects.requireNonNull(model);
		this.files = Objects.requireNonNull(files);
		this.clock = Objects.requireNonNull(clock);
		this.fileChooser = Objects.requireNonNull(fileChooser);
		this.errorReporter = Objects.requireNonNull(errorReporter);
	}

	public void load()
	{
		File file = fileChooser.apply("Load");
		if (file == null)
			return;

		try
		{
			clock.stop();
			model.restore(files.read(file));
		}
		catch (IOException | IllegalArgumentException exception)
		{
			errorReporter.accept("Read Failed!");
		}
	}

	public void store()
	{
		File file = fileChooser.apply("Write");
		if (file == null)
			return;

		try
		{
			clock.stop();
			files.write(file, model.snapshot());
		}
		catch (IOException exception)
		{
			errorReporter.accept("Write Failed!");
		}
	}

	private static File chooseFile(String selectButtonText)
	{
		try
		{
			return Files.userSelected(".", ".life", "Life File", selectButtonText);
		}
		catch (FileNotFoundException cancelled)
		{
			return null;
		}
	}
}
