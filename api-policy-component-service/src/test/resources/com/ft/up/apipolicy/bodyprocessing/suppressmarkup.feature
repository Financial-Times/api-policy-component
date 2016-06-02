@BodyProcessing
Feature: Body processing rules

This is an overview of how the various configuration rules work.

  Scenario Outline:
    Given the unprocessed markup <before>
    When it is transformed
    Then the mark up is removed

  Examples:
   | before                                                                  |
   | <pull-quote><text>Some Text</text><source>source1</source></pull-quote> |
   # empty a tags
   | <a href="http://www.somelink.com"></a>                                  |
   # quotes and quoted things
   | <blockquote class="twitter-tweet" lang="en"><p>Brilliant as always RT <a href="https://twitter.com/DeborahJaneOrr">@DeborahJaneOrr</a>: Will Cornick's 20-year sentence for the killing of Ann Maguire defies logic <a href="http://t.co/93DaG1pAaN">http://t.co/93DaG1pAaN</a></p>&mdash; Graham Linehan (@Glinner) <a href="https://twitter.com/Glinner/status/529965370577526784">November 5, 2014</a></blockquote> |
   | <pull-quote><quote-text>lorem</quote-text><quote-source>author</quote-source></pull-quote> |
   # tables
   | <table class="data-table"><caption>KarCrash Q1  02/2014- period from to 09/2014</caption><tr><th>Sales</th><th>Net profit</th><th>Earnings per share</th><th>Dividend</th></tr><tr><td>€</td><td>€</td><td>€</td><td>€</td></tr><tr><td>324↑ ↓324</td><td>453↑ ↓435</td><td>123↑ ↓989</td><td>748↑↓986</td></tr></table> |
   # big number
   | <big-number><big-number-headline>£350m</big-number-headline><big-number-intro>Cost of the rights expected to increase by one-third — or about £350m a year — although some anticipate inflation of up to 70%</big-number-intro></big-number> |
   # timelines
   | <ft-timeline><timeline-header>Boko Haram timeline: From preachers to slave raiders</timeline-header><timeline-credits>AFP</timeline-credits><timeline-sources>BBC</timeline-sources><timeline-byline>Martin Roddam</timeline-byline><timeline-item><timeline-image height="1152" width="2048"/><timeline-date>2013-05-01 00:00:00</timeline-date><timeline-title>First ‘slaves captured’</timeline-title><timeline-body><p>Boko Haram leader Abubakar Shekau released a video on 13 May 2013, saying Boko Haram had taken women and children - including teenage girls - hostage in response to the arrest of its members’ wives and children.</p></timeline-body></timeline-item><timeline-item><timeline-image height="1152" width="2048"/><timeline-date>2013-02-01 00:00:00</timeline-date><timeline-title>First cross-border raid and kidnapping</timeline-title><timeline-body><p>Now established in remote areas close to Nigeria’s north-eastern border, Boko Haram launched its first operation in neighbouring Cameroon in February 2013.</p></timeline-body></timeline-item><timeline-item><timeline-image height="1152" width="2048"/><timeline-date>2012-07-01 00:000:00</timeline-date><timeline-title>First mosque bombing</timeline-title><timeline-body><p>A suicide bomber detonated himself at a mosque in Maiduguri in July 2012, after Friday prayers. His target was believed to have been the most senior Muslim leader in Borno, Alhaji Abubakar Umar Garbai El-Kanemi.</p></timeline-body></timeline-item><timeline-item><timeline-image height="1152" width="2048"/><timeline-date>2011-06-01 00:00:00</timeline-date><timeline-title>First suicide bombing</timeline-title><timeline-body><p>In June 2011, a suicide bomber rammed a car into the police headquarters in the capital, Abuja, killing eight people. The bomber is alleged to have joined the convoy of then-police chief Gen Hafiz Ringim, before detonating himself.</p></timeline-body></timeline-item><timeline-item><timeline-image height="1152" width="2048"/><timeline-date>2010-12-24 21:00:00</timeline-date><timeline-title>First Christmas bombings</timeline-title><timeline-body><p>At least 32 people were killed in bomb blasts targeting churches on Christmas Eve 2010 in central Plateau state, which straddles Nigeria’s mainly Muslim north and the Christian south.</p></timeline-body></timeline-item><timeline-item><timeline-image height="1152" width="2048"/><timeline-date>2009-06-01 00:000:000</timeline-date><timeline-title>First ever attack</timeline-title><timeline-body><p>Launched in 2002, Boko Haram - which in the local Hausa language means “Western education is forbidden”, a reference to the initial focus of its Islamist agenda - became a force to be reckoned with in 2009 when it raided police stations and government buildings in Maiduguri, and other northern cities.</p></timeline-body></timeline-item></ft-timeline> |
   | <timeline><timeline-header>Boko Haram timeline: From preachers to slave raiders</timeline-header><timeline-credits>AFP</timeline-credits><timeline-sources>BBC</timeline-sources><timeline-byline>Martin Roddam</timeline-byline><timeline-item><timeline-image height="1152" width="2048"/><timeline-date>2013-05-01 00:00:00</timeline-date><timeline-title>First ‘slaves captured’</timeline-title><timeline-body><p>Boko Haram leader Abubakar Shekau released a video on 13 May 2013, saying Boko Haram had taken women and children - including teenage girls - hostage in response to the arrest of its members’ wives and children.</p></timeline-body></timeline-item><timeline-item><timeline-image height="1152" width="2048"/><timeline-date>2013-02-01 00:00:00</timeline-date><timeline-title>First cross-border raid and kidnapping</timeline-title><timeline-body><p>Now established in remote areas close to Nigeria’s north-eastern border, Boko Haram launched its first operation in neighbouring Cameroon in February 2013.</p></timeline-body></timeline-item><timeline-item><timeline-image height="1152" width="2048"/><timeline-date>2012-07-01 00:000:00</timeline-date><timeline-title>First mosque bombing</timeline-title><timeline-body><p>A suicide bomber detonated himself at a mosque in Maiduguri in July 2012, after Friday prayers. His target was believed to have been the most senior Muslim leader in Borno, Alhaji Abubakar Umar Garbai El-Kanemi.</p></timeline-body></timeline-item><timeline-item><timeline-image height="1152" width="2048"/><timeline-date>2011-06-01 00:00:00</timeline-date><timeline-title>First suicide bombing</timeline-title><timeline-body><p>In June 2011, a suicide bomber rammed a car into the police headquarters in the capital, Abuja, killing eight people. The bomber is alleged to have joined the convoy of then-police chief Gen Hafiz Ringim, before detonating himself.</p></timeline-body></timeline-item><timeline-item><timeline-image height="1152" width="2048"/><timeline-date>2010-12-24 21:00:00</timeline-date><timeline-title>First Christmas bombings</timeline-title><timeline-body><p>At least 32 people were killed in bomb blasts targeting churches on Christmas Eve 2010 in central Plateau state, which straddles Nigeria’s mainly Muslim north and the Christian south.</p></timeline-body></timeline-item><timeline-item><timeline-image height="1152" width="2048"/><timeline-date>2009-06-01 00:000:000</timeline-date><timeline-title>First ever attack</timeline-title><timeline-body><p>Launched in 2002, Boko Haram - which in the local Hausa language means “Western education is forbidden”, a reference to the initial focus of its Islamist agenda - became a force to be reckoned with in 2009 when it raided police stations and government buildings in Maiduguri, and other northern cities.</p></timeline-body></timeline-item></timeline> |
   # images
   | <ft-content type="http://www.ft.com/ontology/content/ImageSet">Portrait of the president</ft-content>|
   | <img alt="Saloua Raouda Choucair's ‘Composition'" height="445" src="http://im.ft-static.com/content/images/7784185e-a888-11e2-8e5d-00144feabdc0.img" width="600"/>|
   # videos
   | <ft-content data-embedded="true" type="http://www.ft.com/ontology/content/MediaResource" url="http://api.ft.com/content/807a95c5-87c3-3aee-9239-387f6dc32a60"></ft-content>|
   # promoboxes
   | <promo-box><promo-title><p><a href="http://www.ft.com/reports/ft-500-2011" title="www.ft.com">FT 500</a></p></promo-title><promo-headline><p>Headline</p></promo-headline><promo-image><content data-embedded="true" id="432b5632-9e79-11e0-0a0f-978e959e1689" type="http://www.ft.com/ontology/content/ImageSet"></content></promo-image><promo-intro><p>The risers and fallers in our annual list of the world’s biggest companies</p></promo-intro><promo-link><p><a href="http://www.ft.com/cms/s/0/0bdf4bb6-6676-11e4-8bf6-00144feabdc0.html"></a></p></promo-link></promo-box> |
   | <ft-related type="http://www.ft.com/ontology/content/Article" url="api.ft.com/content/abc123">Profile of the president</ft-related>|