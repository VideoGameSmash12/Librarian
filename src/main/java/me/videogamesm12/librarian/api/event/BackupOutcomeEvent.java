package me.videogamesm12.librarian.api.event;

import lombok.Getter;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;

import java.io.File;

@Getter
public class BackupOutcomeEvent extends LibrarianEvent
{
	private final Throwable exception;
	private final IWrappedHotbarStorage storage;
	private final File path;

	public BackupOutcomeEvent(IWrappedHotbarStorage storage, Throwable ex)
	{
		this.storage = storage;
		this.exception = ex;
		this.path = null;
	}

	public BackupOutcomeEvent(IWrappedHotbarStorage storage, File path)
	{
		this.storage = storage;
		this.exception = null;
		this.path = path;
	}
}
