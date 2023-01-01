package consulo.xml.codeInsight.daemon.impl.quickfix;

import consulo.logging.Logger;
import consulo.project.content.WatchedRootsProvider;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Collections;
import java.util.Set;

/**
 * @author VISTALL
 * @since 14-Jul-22
 */
public class FetchExResourceWatchedRootsProvider implements WatchedRootsProvider
{
	private static final Logger LOG = Logger.getInstance(FetchExResourceWatchedRootsProvider.class);

	@Nonnull
	@Override
	public Set<String> getRootsToWatch()
	{
		final File path = new File(FetchExtResourceAction.getExternalResourcesPath());
		if(!path.exists() && !path.mkdirs())
		{
			LOG.warn("Unable to create: " + path);
		}
		return Collections.singleton(path.getAbsolutePath());
	}
}
